import {messaging} from "../config/firebase";
import {getProfileTokens} from "./tokens";

export type MessageData = Record<string, string | number | boolean>;

export async function sendToProfile(
  profileId: string,
  data: MessageData,
): Promise<void> {
  const tokens = await getProfileTokens(profileId);

  if (tokens.length === 0) return;

  const payload: Record<string, string> = {};

  for (const [k, v] of Object.entries(data)) {
    payload[k] = String(v);
  }

  await messaging.sendEachForMulticast({
    tokens,
    data: payload,
  });
}
