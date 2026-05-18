export type ScheduleType = 'ONE_TIME' | 'RECURRING' | 'WEEKLY' | 'CRON';

export interface TaskParameterSchema {
  name: string;
  type: string;
  required: boolean;
  description: string;
}

export interface TaskResponse {
  id: string;
  name: string;
  description: string;
  parameterSchema: TaskParameterSchema[];
}

export interface ScheduleResponse {
  id: string;
  taskId: string;
  taskName: string;
  scheduleType: ScheduleType;
  cronExpression: string | null;
  intervalSeconds: number | null;
  daysOfWeek: string | null;
  startTime: string;
  endTime: string | null;
  taskParameters: Record<string, unknown> | null;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleRequest {
  taskId: string;
  scheduleType: ScheduleType;
  cronExpression?: string | null;
  intervalSeconds?: number | null;
  daysOfWeek?: string | null;
  startTime: string;
  endTime?: string | null;
  taskParameters?: Record<string, unknown>;
  enabled: boolean;
}
