import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  FormControlLabel,
  Switch,
  Box,
  Typography,
  ToggleButton,
  ToggleButtonGroup,
  Alert,
} from '@mui/material';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import dayjs, { Dayjs } from 'dayjs';
import { createSchedule, updateSchedule } from '../api/schedules';
import { ScheduleRequest, ScheduleResponse, ScheduleType, TaskResponse } from '../types';

interface ScheduleDialogProps {
  open: boolean;
  onClose: (saved: boolean) => void;
  tasks: TaskResponse[];
  schedule: ScheduleResponse | null;
}

const DAYS_OF_WEEK = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

const SCHEDULE_TYPE_OPTIONS: { value: ScheduleType; label: string }[] = [
  { value: 'ONE_TIME', label: 'One-Time' },
  { value: 'RECURRING', label: 'Recurring' },
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'CRON', label: 'Cron Expression' },
];

export default function ScheduleDialog({ open, onClose, tasks, schedule }: ScheduleDialogProps) {
  const isEdit = schedule !== null;

  const [taskId, setTaskId] = useState('');
  const [scheduleType, setScheduleType] = useState<ScheduleType>('ONE_TIME');
  const [startTime, setStartTime] = useState<Dayjs | null>(dayjs().add(1, 'hour'));
  const [endTime, setEndTime] = useState<Dayjs | null>(null);
  const [intervalSeconds, setIntervalSeconds] = useState<number>(300);
  const [intervalUnit, setIntervalUnit] = useState<'seconds' | 'minutes' | 'hours'>('minutes');
  const [daysOfWeek, setDaysOfWeek] = useState<string[]>([]);
  const [cronExpression, setCronExpression] = useState('');
  const [taskParams, setTaskParams] = useState<Record<string, string>>({});
  const [enabled, setEnabled] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;

    if (schedule) {
      setTaskId(schedule.taskId);
      setScheduleType(schedule.scheduleType);
      setStartTime(dayjs(schedule.startTime));
      setEndTime(schedule.endTime ? dayjs(schedule.endTime) : null);
      setIntervalSeconds(schedule.intervalSeconds ?? 300);
      setDaysOfWeek(schedule.daysOfWeek ? schedule.daysOfWeek.split(',') : []);
      setCronExpression(schedule.cronExpression ?? '');
      setEnabled(schedule.enabled);

      if (schedule.taskParameters) {
        const stringParams: Record<string, string> = {};
        for (const [k, v] of Object.entries(schedule.taskParameters)) {
          stringParams[k] = String(v ?? '');
        }
        setTaskParams(stringParams);
      } else {
        setTaskParams({});
      }

      if (schedule.intervalSeconds) {
        if (schedule.intervalSeconds >= 3600 && schedule.intervalSeconds % 3600 === 0) {
          setIntervalUnit('hours');
          setIntervalSeconds(schedule.intervalSeconds / 3600);
        } else if (schedule.intervalSeconds >= 60 && schedule.intervalSeconds % 60 === 0) {
          setIntervalUnit('minutes');
          setIntervalSeconds(schedule.intervalSeconds / 60);
        } else {
          setIntervalUnit('seconds');
          setIntervalSeconds(schedule.intervalSeconds);
        }
      }
    } else {
      setTaskId(tasks.length > 0 ? tasks[0].id : '');
      setScheduleType('ONE_TIME');
      setStartTime(dayjs().add(1, 'hour'));
      setEndTime(null);
      setIntervalSeconds(5);
      setIntervalUnit('minutes');
      setDaysOfWeek([]);
      setCronExpression('');
      setTaskParams({});
      setEnabled(true);
    }
    setError(null);
  }, [open, schedule, tasks]);

  const selectedTask = tasks.find((t) => t.id === taskId);

  const computeIntervalInSeconds = (): number => {
    switch (intervalUnit) {
      case 'hours': return intervalSeconds * 3600;
      case 'minutes': return intervalSeconds * 60;
      default: return intervalSeconds;
    }
  };

  const handleSave = async () => {
    setError(null);
    setSaving(true);

    try {
      const taskParameters: Record<string, unknown> = {};
      if (selectedTask) {
        for (const param of selectedTask.parameterSchema) {
          const val = taskParams[param.name] ?? '';
          if (param.type === 'number') {
            taskParameters[param.name] = val ? Number(val) : null;
          } else {
            taskParameters[param.name] = val;
          }
        }
      }

      const request: ScheduleRequest = {
        taskId,
        scheduleType,
        startTime: (startTime ?? dayjs()).format('YYYY-MM-DDTHH:mm:ss'),
        endTime: endTime ? endTime.format('YYYY-MM-DDTHH:mm:ss') : null,
        cronExpression: scheduleType === 'CRON' ? cronExpression : null,
        intervalSeconds: scheduleType === 'RECURRING' ? computeIntervalInSeconds() : null,
        daysOfWeek: scheduleType === 'WEEKLY' ? daysOfWeek.join(',') : null,
        taskParameters,
        enabled,
      };

      if (isEdit && schedule) {
        await updateSchedule(schedule.id, request);
      } else {
        await createSchedule(request);
      }

      onClose(true);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to save schedule';
      if (typeof err === 'object' && err !== null && 'response' in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr.response?.data?.message ?? msg);
      } else {
        setError(msg);
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={() => onClose(false)} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Edit Schedule' : 'Create Schedule'}</DialogTitle>
      <DialogContent dividers>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}

          {/* Task Selection */}
          <FormControl fullWidth>
            <InputLabel>Task</InputLabel>
            <Select
              value={taskId}
              label="Task"
              onChange={(e) => {
                setTaskId(e.target.value);
                setTaskParams({});
              }}
            >
              {tasks.map((t) => (
                <MenuItem key={t.id} value={t.id}>
                  {t.name} — {t.description}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {/* Schedule Type */}
          <FormControl fullWidth>
            <InputLabel>Schedule Type</InputLabel>
            <Select
              value={scheduleType}
              label="Schedule Type"
              onChange={(e) => setScheduleType(e.target.value as ScheduleType)}
            >
              {SCHEDULE_TYPE_OPTIONS.map((opt) => (
                <MenuItem key={opt.value} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {/* Start Time (all types) */}
          <DateTimePicker
            label="Start Time"
            value={startTime}
            onChange={setStartTime}
            slotProps={{ textField: { fullWidth: true } }}
          />

          {/* Recurring: interval */}
          {scheduleType === 'RECURRING' && (
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                label="Interval"
                type="number"
                sx={{ flex: 7 }}
                value={intervalSeconds}
                onChange={(e) => setIntervalSeconds(Math.max(1, Number(e.target.value)))}
                slotProps={{ htmlInput: { min: 1 } }}
              />
              <FormControl sx={{ flex: 5 }}>
                <InputLabel>Unit</InputLabel>
                <Select
                  value={intervalUnit}
                  label="Unit"
                  onChange={(e) => setIntervalUnit(e.target.value as 'seconds' | 'minutes' | 'hours')}
                >
                  <MenuItem value="seconds">Seconds</MenuItem>
                  <MenuItem value="minutes">Minutes</MenuItem>
                  <MenuItem value="hours">Hours</MenuItem>
                </Select>
              </FormControl>
            </Box>
          )}

          {/* Recurring: end time */}
          {scheduleType === 'RECURRING' && (
            <DateTimePicker
              label="End Time (optional)"
              value={endTime}
              onChange={setEndTime}
              slotProps={{ textField: { fullWidth: true } }}
            />
          )}

          {/* Weekly: day picker */}
          {scheduleType === 'WEEKLY' && (
            <Box>
              <Typography variant="body2" sx={{ mb: 1 }}>
                Days of Week
              </Typography>
              <ToggleButtonGroup
                value={daysOfWeek}
                onChange={(_e, newDays: string[]) => setDaysOfWeek(newDays)}
                aria-label="days of week"
                size="small"
              >
                {DAYS_OF_WEEK.map((day) => (
                  <ToggleButton key={day} value={day}>
                    {day}
                  </ToggleButton>
                ))}
              </ToggleButtonGroup>
            </Box>
          )}

          {/* Cron: expression */}
          {scheduleType === 'CRON' && (
            <TextField
              label="Cron Expression"
              fullWidth
              value={cronExpression}
              onChange={(e) => setCronExpression(e.target.value)}
              placeholder="0 0/15 * * * ?"
              helperText="Quartz cron format: sec min hour dayOfMonth month dayOfWeek"
            />
          )}

          {/* Dynamic Task Parameters */}
          {selectedTask && selectedTask.parameterSchema.length > 0 && (
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>
                Task Parameters
              </Typography>
              {selectedTask.parameterSchema.map((param) => (
                <TextField
                  key={param.name}
                  label={`${param.name}${param.required ? ' *' : ''}`}
                  fullWidth
                  type={param.type === 'number' ? 'number' : 'text'}
                  value={taskParams[param.name] ?? ''}
                  onChange={(e) =>
                    setTaskParams((prev) => ({ ...prev, [param.name]: e.target.value }))
                  }
                  helperText={param.description}
                  required={param.required}
                  sx={{ mb: 1.5 }}
                />
              ))}
            </Box>
          )}

          {/* Enabled toggle */}
          <FormControlLabel
            control={<Switch checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />}
            label="Enabled"
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => onClose(false)}>Cancel</Button>
        <Button onClick={handleSave} variant="contained" disabled={saving}>
          {saving ? 'Saving...' : isEdit ? 'Update' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
