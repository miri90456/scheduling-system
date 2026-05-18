import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import ScheduleDialog from './ScheduleDialog';
import { TaskResponse } from '../types';

vi.mock('../api/schedules', () => ({
  createSchedule: vi.fn().mockResolvedValue({ id: '1' }),
  updateSchedule: vi.fn().mockResolvedValue({ id: '1' }),
}));

const theme = createTheme();

const mockTasks: TaskResponse[] = [
  {
    id: 'log-task',
    name: 'Log Task',
    description: 'Writes a message to the log',
    parameterSchema: [
      { name: 'message', type: 'string', required: true, description: 'The message to log' },
    ],
  },
  {
    id: 'http-ping-task',
    name: 'HTTP Ping Task',
    description: 'Pings a URL',
    parameterSchema: [
      { name: 'url', type: 'string', required: true, description: 'The URL to ping' },
      { name: 'timeout', type: 'number', required: false, description: 'Timeout in seconds' },
    ],
  },
];

function renderDialog(props: Partial<React.ComponentProps<typeof ScheduleDialog>> = {}) {
  return render(
    <ThemeProvider theme={theme}>
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <ScheduleDialog
          open={true}
          onClose={vi.fn()}
          tasks={mockTasks}
          schedule={null}
          {...props}
        />
      </LocalizationProvider>
    </ThemeProvider>
  );
}

describe('ScheduleDialog', () => {
  it('should render create dialog with task selector', () => {
    renderDialog();
    expect(screen.getByText('Create Schedule')).toBeInTheDocument();
  });

  it('should show edit title when schedule is provided', () => {
    renderDialog({
      schedule: {
        id: '123',
        taskId: 'log-task',
        taskName: 'Log Task',
        scheduleType: 'ONE_TIME',
        cronExpression: null,
        intervalSeconds: null,
        daysOfWeek: null,
        startTime: '2026-06-01T10:00:00',
        endTime: null,
        taskParameters: { message: 'hello' },
        enabled: true,
        createdAt: '2026-01-01T00:00:00',
        updatedAt: '2026-01-01T00:00:00',
      },
    });
    expect(screen.getByText('Edit Schedule')).toBeInTheDocument();
  });

  it('should show task parameter fields for the selected task', () => {
    renderDialog();
    expect(screen.getByLabelText(/message \*/i)).toBeInTheDocument();
  });

  it('should render Cancel and Create buttons', () => {
    renderDialog();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create/i })).toBeInTheDocument();
  });

  it('should not render when closed', () => {
    renderDialog({ open: false });
    expect(screen.queryByText('Create Schedule')).not.toBeInTheDocument();
  });
});
