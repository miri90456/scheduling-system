import { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Button,
  Chip,
  IconButton,
  Tooltip,
  Alert,
  Snackbar,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { fetchSchedules, deleteSchedule } from '../api/schedules';
import { fetchTasks } from '../api/tasks';
import { ScheduleResponse, TaskResponse, ScheduleType } from '../types';
import DeleteConfirmDialog from './DeleteConfirmDialog';
import ScheduleDialog from './ScheduleDialog';

const scheduleTypeLabels: Record<ScheduleType, string> = {
  ONE_TIME: 'One-Time',
  RECURRING: 'Recurring',
  WEEKLY: 'Weekly',
  CRON: 'Cron',
};

function formatScheduleDetails(row: ScheduleResponse): string {
  switch (row.scheduleType) {
    case 'ONE_TIME':
      return new Date(row.startTime).toLocaleString();
    case 'RECURRING': {
      const secs = row.intervalSeconds ?? 0;
      if (secs >= 3600) return `Every ${secs / 3600}h`;
      if (secs >= 60) return `Every ${secs / 60}m`;
      return `Every ${secs}s`;
    }
    case 'WEEKLY':
      return row.daysOfWeek ?? '';
    case 'CRON':
      return row.cronExpression ?? '';
    default:
      return '';
  }
}

export default function ScheduleTable() {
  const [schedules, setSchedules] = useState<ScheduleResponse[]>([]);
  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ScheduleResponse | null>(null);
  const [snackbar, setSnackbar] = useState<{ message: string; severity: 'success' | 'error' } | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [schedulesData, tasksData] = await Promise.all([fetchSchedules(), fetchTasks()]);
      setSchedules(schedulesData);
      setTasks(tasksData);
    } catch {
      setSnackbar({ message: 'Failed to load data', severity: 'error' });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteSchedule(deleteTarget);
      setSnackbar({ message: 'Schedule deleted', severity: 'success' });
      setDeleteTarget(null);
      loadData();
    } catch {
      setSnackbar({ message: 'Failed to delete schedule', severity: 'error' });
    }
  };

  const handleDialogClose = (saved: boolean) => {
    setDialogOpen(false);
    setEditTarget(null);
    if (saved) {
      loadData();
      setSnackbar({ message: editTarget ? 'Schedule updated' : 'Schedule created', severity: 'success' });
    }
  };

  const columns: GridColDef[] = [
    {
      field: 'taskName',
      headerName: 'Task',
      flex: 1,
      minWidth: 150,
    },
    {
      field: 'scheduleType',
      headerName: 'Type',
      width: 130,
      renderCell: (params) => (
        <Chip
          label={scheduleTypeLabels[params.value as ScheduleType] ?? params.value}
          size="small"
          variant="outlined"
        />
      ),
    },
    {
      field: 'details',
      headerName: 'Schedule Details',
      flex: 1,
      minWidth: 180,
      valueGetter: (_value, row) => formatScheduleDetails(row),
    },
    {
      field: 'enabled',
      headerName: 'Status',
      width: 110,
      renderCell: (params) => (
        <Chip
          label={params.value ? 'Active' : 'Disabled'}
          color={params.value ? 'success' : 'default'}
          size="small"
        />
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      width: 170,
      valueFormatter: (value: string) => new Date(value).toLocaleString(),
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 120,
      sortable: false,
      filterable: false,
      renderCell: (params) => (
        <Box>
          <Tooltip title="Edit">
            <IconButton
              size="small"
              onClick={() => {
                setEditTarget(params.row);
                setDialogOpen(true);
              }}
            >
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton
              size="small"
              color="error"
              onClick={() => setDeleteTarget(params.row.id)}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      ),
    },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => {
            setEditTarget(null);
            setDialogOpen(true);
          }}
        >
          New Schedule
        </Button>
      </Box>

      <DataGrid
        rows={schedules}
        columns={columns}
        loading={loading}
        pageSizeOptions={[10, 25, 50]}
        initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
        disableRowSelectionOnClick
        autoHeight
        sx={{
          bgcolor: 'background.paper',
          borderRadius: 2,
          '& .MuiDataGrid-columnHeaders': { bgcolor: 'grey.50' },
        }}
      />

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
      />

      <ScheduleDialog
        open={dialogOpen}
        onClose={handleDialogClose}
        tasks={tasks}
        schedule={editTarget}
      />

      <Snackbar
        open={snackbar !== null}
        autoHideDuration={4000}
        onClose={() => setSnackbar(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        {snackbar ? (
          <Alert severity={snackbar.severity} onClose={() => setSnackbar(null)} variant="filled">
            {snackbar.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </Box>
  );
}
