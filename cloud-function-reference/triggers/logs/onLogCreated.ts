import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {sendToProfile} from "../../helpers/messaging";

interface LogData {
  scheduleId: string;
  status: string;
  scheduledTime: FirebaseFirestore.Timestamp;
}

export const onLogCreated = onDocumentCreated(
  "profiles/{profileId}/logs/{logId}",
  async (event) => {
    const {profileId, logId} = event.params;
    const log = event.data?.data() as LogData;

    if (!log) return;

    await sendToProfile(profileId, {
      type: "alarm_event",
      logId,
      scheduleId: log.scheduleId,
      status: log.status,
      scheduledTime: log.scheduledTime.toMillis(),
    });
  },
);
