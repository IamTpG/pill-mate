export const EVENTS = {
  SCHEDULE_CREATED: "schedule_created",
  SCHEDULE_UPDATED: "schedule_updated",
  SCHEDULE_DELETED: "schedule_deleted",
  ALARM_EVENT: "alarm_event",
} as const;

export type EventType = (typeof EVENTS)[keyof typeof EVENTS];
