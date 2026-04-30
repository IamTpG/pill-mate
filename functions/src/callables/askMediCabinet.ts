import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import Groq from "groq-sdk";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
    admin.initializeApp();
}

const groqApiKey = defineSecret("GROQ_API_KEY");

// Tool: The Database Fetcher
async function fetchUserCabinetData(uid: string): Promise<string> {
    const profileRef = admin.firestore().collection("profiles").doc(uid);
    const [profileDoc, medicationsSnapshot, schedulesSnapshot] = await Promise.all([
        profileRef.get(),
        profileRef.collection("medications").get(),
        profileRef.collection("schedules").get(),
    ]);

    if (!profileDoc.exists) return "No profile found.";

    // 1. Detailed Medication Mapping
    const medications = medicationsSnapshot.docs
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

    return [
        medications.length > 0 ? `Cabinet medications: ${medications.join("; ")}.` : "Cabinet medications: none.",
        schedules.length > 0 ? `Medication schedules: ${schedules.join("; ")}.` : "Medication schedules: none."
    ].join(" ");
}

// Main Function
export const askMediCabinet = onCall(
    { secrets: [groqApiKey] },
    async (request) => {
        if (!request.auth) {
            throw new HttpsError("unauthenticated", "You must be logged in.");
        }

        const uid = request.auth.uid;
        const userMessage = request.data.message;

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
                    - If the user asks what is in their cabinet, ACTUALLY LIST the items found in the data!`
                },
                {
                    role: "user",
                    content: userMessage
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

                    console.log("AI requested database. Fetching now...");
                    const realDatabaseData = await fetchUserCabinetData(uid);

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