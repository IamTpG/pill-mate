import {db} from "../config/firebase";

export async function getProfileTokens(profileId: string): Promise<string[]> {
  const snapshot = await db
    .collection("profiles")
    .doc(profileId)
    .collection("fcmTokens")
    .get();

  const tokens: string[] = [];

  snapshot.forEach((doc) => tokens.push(doc.id));

  return tokens;
}
