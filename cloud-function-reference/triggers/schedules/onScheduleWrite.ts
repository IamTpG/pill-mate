import {onDocumentWritten} from "firebase-functions/v2/firestore";
import {sendToProfile} from "../../helpers/messaging";
import {EVENTS, EventType} from "../../types/events";

export const onScheduleWrite = onDocumentWritten(
  "profiles/{profileId}/schedules/{scheduleId}",
  async (event) => {
    const {profileId, scheduleId} = event.params;
    const change = event.data;

    if (!change) return;

    let type: EventType = EVENTS.SCHEDULE_UPDATED;

    if (!change.after.exists) {
      type = EVENTS.SCHEDULE_DELETED;
    } else if (!change.before.exists) {
      type = EVENTS.SCHEDULE_CREATED;
    }

    await sendToProfile(profileId, {type, scheduleId});
  },
);
