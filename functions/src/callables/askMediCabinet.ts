import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import Groq from "groq-sdk";
import * as admin from "firebase-admin";

// 1. Initialize Firebase Admin (Required to read your database)
if (!admin.apps.length) {
    admin.initializeApp();
}

const groqApiKey = defineSecret("GROQ_API_KEY");

export const askMediCabinet = onCall(
    { secrets: [groqApiKey] },
    async (request) => {
        // SECURITY CHECK: Kick out anyone who isn't logged into the Android app
        if (!request.auth) {
            throw new HttpsError(
                "unauthenticated",
                "You must be logged in to ask the AI."
            );
        }

        const uid = request.auth.uid; // The secure ID of the user asking the question
        const userMessage = request.data.message;

        try {
            // 2. FETCH REAL DATA: profiles/{uid}/medications + profiles/{uid}/schedules
            const profileRef = admin.firestore().collection("profiles").doc(uid);
            const [profileDoc, medicationsSnapshot, schedulesSnapshot] = await Promise.all([
                profileRef.get(),
                profileRef.collection("medications").get(),
                profileRef.collection("schedules").get(),
            ]);

            if (!profileDoc.exists) {
                throw new HttpsError("not-found", "Profile not found for current user.");
            }

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

                    const summary = [
                        scheduleName || eventTitle || "Unnamed schedule",
                        eventDose !== null ? `dose ${eventDose}` : "",
                        eventUnit ? eventUnit : "",
                        doseTimes ? `times: ${doseTimes}` : "",
                        recurrenceRule ? `rrule: ${recurrenceRule}` : "",
                    ].filter(Boolean).join(" | ");

                    return summary;
                })
                .filter(Boolean);

            const realDatabaseData = [
                medications.length > 0
                    ? `Cabinet medications: ${medications.join("; ")}.`
                    : "Cabinet medications: none.",
                schedules.length > 0
                    ? `Medication schedules: ${schedules.join("; ")}.`
                    : "Medication schedules: none.",
            ].join(" ");
            const normalizedMessage = String(userMessage ?? "").toLowerCase();
            const isCabinetOrScheduleQuestion = [
                "my cabinet",
                "in my cabinet",
                "my medication",
                "my medications",
                "my meds",
                "my pills",
                "what am i taking",
                "do i have",
                "my schedule",
                "my schedules",
                "my reminder",
                "my reminders",
            ].some((keyword) => normalizedMessage.includes(keyword));

            // 3. INIT GROQ
            const groq = new Groq({ apiKey: groqApiKey.value() });

            // 4. TALK TO AI WITH REAL CONTEXT
            const chatCompletion = await groq.chat.completions.create({
                messages: [
                    {
                        role: "system",
                        content: `You are MediCabinet, a helpful medical AI.
Question mode: ${isCabinetOrScheduleQuestion ? "USER_SPECIFIC" : "GENERAL_KNOWLEDGE"}.
If mode is USER_SPECIFIC:
- Use only cabinet and schedule data from Context.
- Never invent cabinet contents, doses, schedules, interactions, or account-specific facts.
- If Context says cabinet medications are none, respond that cabinet is empty and ask user to add medications first.
- If asked about meds/schedules not present in Context, clearly say that information is not in cabinet data.
If mode is GENERAL_KNOWLEDGE:
- You may answer using general medical/pharmacy knowledge.
- Keep answer factual, concise, and include brief safety note when relevant.
Context: ${realDatabaseData}`
                    },
                    {
                        role: "user",
                        content: userMessage
                    }
                ],
                model: "llama-3.1-8b-instant",
            });

            const aiReply = chatCompletion.choices[0]?.message?.content || "No reply generated.";

            return { reply: aiReply };

        } catch (error) {
            console.error("Server Error:", error);
            throw new HttpsError("internal", "Failed to process the request.");
        }
    }
);