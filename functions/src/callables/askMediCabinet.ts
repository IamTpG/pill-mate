import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import Groq from "groq-sdk";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
    admin.initializeApp();
}

const groqApiKey = defineSecret("GROQ_API_KEY");

function hasDeletedAtField(data: admin.firestore.DocumentData | undefined): boolean {
    return data?.deletedAt != null;
}

async function assertCallerAccessToProfile(profileId: string, authUid: string): Promise<void> {
    if (profileId === authUid) {
        return;
    }
    const doc = await admin.firestore().collection("profiles").doc(profileId).get();
    if (!doc.exists) {
        throw new HttpsError("not-found", "Profile not found.");
    }
    const accountId = doc.data()?.accountId;
    if (typeof accountId === "string" && accountId === authUid) {
        return;
    }
    const caregiverSnap = await admin
        .firestore()
        .collection("profiles")
        .doc(profileId)
        .collection("caregiverLinks")
        .where("caregiverAccountId", "==", authUid)
        .limit(1)
        .get();
    if (!caregiverSnap.empty) {
        return;
    }
    throw new HttpsError("permission-denied", "Cannot access this profile.");
}

// Tool: The Database Fetcher
async function fetchUserCabinetData(profileId: string): Promise<string> {
    const profileRef = admin.firestore().collection("profiles").doc(profileId);
    const [profileDoc, medicationsSnapshot, schedulesSnapshot] = await Promise.all([
        profileRef.get(),
        profileRef.collection("medications").get(),
        profileRef.collection("schedules").get(),
    ]);

    if (!profileDoc.exists) {
        return "DATABASE RESULT: User profile does not exist yet. Cabinet is empty.";
    }

    // 1. Detailed Medication Mapping
    const medications = medicationsSnapshot.docs
        .filter((doc) => !hasDeletedAtField(doc.data()))
        .map((doc) => {
            const data = doc.data();
            const name = typeof data.name === "string" ? data.name : "";
            const unit = typeof data.unit === "string" ? data.unit : "";
            const description = typeof data.description === "string" ? data.description : "";

            if (!name) return null;
            const pieces = [name, unit ? `(${unit})` : "", description ? `- ${description}` : ""]
                .filter(Boolean)
                .join(" ");
            return pieces.trim();
        })
        .filter((item): item is string => item !== null);

    // 2. Detailed Schedule & Dose Mapping
    const schedules = schedulesSnapshot.docs
        .filter((doc) => !hasDeletedAtField(doc.data()))
        .map((doc) => {
            const data = doc.data();
            const scheduleName = typeof data.name === "string" ? data.name : "";
            const recurrenceRule = typeof data.recurrenceRule === "string" ? data.recurrenceRule : "";
            const eventSnapshot = data.eventSnapshot as Record<string, unknown> | undefined;
            const eventTitle = typeof eventSnapshot?.title === "string" ? eventSnapshot.title : "";
            const eventDose = typeof eventSnapshot?.dose === "number" ? eventSnapshot.dose : null;
            const eventUnit = typeof eventSnapshot?.unit === "string" ? eventSnapshot.unit : "";

            const doseTimes = Array.isArray(data.doseTimes)
                ? data.doseTimes
                    .map((item) => {
                        if (!item || typeof item !== "object") return "";
                        const t = item as Record<string, unknown>;
                        const time = typeof t.time === "string" ? t.time : "";
                        const dose = typeof t.dose === "number" ? t.dose : null;
                        return [time, dose !== null ? `(dose ${dose})` : ""].filter(Boolean).join(" ");
                    })
                    .filter(Boolean)
                    .join(", ")
                : "";

            return [
                scheduleName || eventTitle || "Unnamed schedule",
                eventDose !== null ? `dose ${eventDose}` : "",
                eventUnit ? eventUnit : "",
                doseTimes ? `times: ${doseTimes}` : "",
                recurrenceRule ? `rrule: ${recurrenceRule}` : "",
            ].filter(Boolean).join(" | ");
        })
        .filter(Boolean);
    if (medications.length === 0 && schedules.length === 0) {
        return "DATABASE RESULT: The user's cabinet is completely empty. No medications or schedules found.";
    }

    return [
        "DATABASE RESULT:",
        medications.length > 0 ? `Cabinet medications: ${medications.join("; ")}.` : "Cabinet medications: none.",
        schedules.length > 0 ? `Medication schedules: ${schedules.join("; ")}.` : "Medication schedules: none."
    ].join("\n");
}

// Main Function
export const askMediCabinet = onCall(
    { secrets: [groqApiKey] },
    async (request) => {
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "You must be logged in.");
        }
        const rawPid = request.data?.profileId;
        const trimmed = typeof rawPid === "string" ? rawPid.trim() : "";
        const requestedProfileId = trimmed || request.auth.uid;
        await assertCallerAccessToProfile(requestedProfileId, request.auth.uid);
        const activeProfileId = requestedProfileId;
        const userMessage = request.data?.message;
        if (typeof userMessage !== "string" || !userMessage.trim()) {
            throw new HttpsError("invalid-argument", "Message is required.");
        }
        const trimmedMessage = userMessage.trim();

        try {
            const groq = new Groq({ apiKey: groqApiKey.value() });

            // Conversation History
            const messages: any[] = [
                {
                    role: "system",
                    content: `You are MediCabinet, a medical AI assistant for PillMate. 
                    Routing rules:
                    1) GENERAL MEDICAL KNOWLEDGE questions (example: "what is aspirin") -> answer using medical knowledge.
                    2) USER-SPECIFIC ACCOUNT questions about "my meds", "my cabinet", "my schedule", "what am I taking", "do I have X", "my reminders" -> YOU MUST call 'get_user_cabinet_data' tool before answering.
                    3) APP HOW-TO questions (example: "how do I add medication", "how to create schedule", "how do I set reminder in app") -> answer using APP INSTRUCTION MANUAL below. Do NOT call database tool unless user also asks about their personal data.
                    APP INSTRUCTION MANUAL (PillMate):
                    - Add medication to cabinet:
                      1. Open 'Cabinet' tab.
                      2. Tap '+' floating button at bottom-right.
                      3. Fill medication form (name, unit, count, description, expiration, optional image).
                      4. Tap Save/Add in dialog.
                    - Create medication schedule:
                      1. Open 'Home' tab.
                      2. Tap '+' icon in top-right header.
                      3. In flow, pick medication.
                      4. On schedule list screen, tap '+' to add new schedule.
                      5. Set reminder times, frequency, start/end date, then save.
                    - Edit/Delete schedule:
                      1. Open same schedule flow from Home '+'.
                      2. Select medication, choose existing schedule to view/edit or delete.

                    CRITICAL BEHAVIOR CONSTRAINTS:
                    - NEVER narrate your internal processes.
                    - NEVER use the words "routing rule", "tool", "get_user_cabinet_data", or "APP INSTRUCTION MANUAL" in your replies to the user.
                    - NEVER say "I have called the tool" or "Based on the information retrieved".
                    - Just answer the question directly, naturally, and conversationally.
                    - If the user asks what is in their cabinet, ACTUALLY LIST the items found in the data!
                    - ESCAPE HATCH: If the database tool returns that the cabinet is EMPTY or has no medications, DO NOT invent duplicates or say the data is unclear. Simply tell the user their cabinet is empty, and politely offer to help them add a medication using the App Manual steps.`
                },
                {
                    role: "user",
                    content: trimmedMessage
                }
            ];

            // Define the Tool
            const tools = [
                {
                    type: "function",
                    function: {
                        name: "get_user_cabinet_data",
                        description: "Fetches the user's actual medication inventory and alarm schedules from the database.",
                        parameters: { type: "object", properties: {} }
                    }
                }
            ];

            // API CALL #1
            const firstResponse = await groq.chat.completions.create({
                model: "llama-3.1-8b-instant",
                messages: messages,
                tools: tools,
                tool_choice: "auto",
            });

            const responseMessage = firstResponse.choices[0]?.message;

            if (responseMessage?.tool_calls) {
                messages.push(responseMessage);
                const toolCall = responseMessage.tool_calls[0];

                if (toolCall.function.name === "get_user_cabinet_data") {

                    console.log(`AI requested database for profile: ${activeProfileId}. Fetching now...`);
                    const realDatabaseData = await fetchUserCabinetData(activeProfileId);

                    messages.push({
                        role: "tool",
                        tool_call_id: toolCall.id,
                        content: realDatabaseData
                    });

                    // API CALL #2
                    const secondResponse = await groq.chat.completions.create({
                        model: "llama-3.1-8b-instant",
                        messages: messages,
                    });

                    return { reply: secondResponse.choices[0]?.message?.content };
                }
            }

            return { reply: responseMessage?.content || "No reply generated." };

        } catch (error) {
            console.error("Server Error:", error);
            throw new HttpsError("internal", "Failed to process the request.");
        }
    }
);