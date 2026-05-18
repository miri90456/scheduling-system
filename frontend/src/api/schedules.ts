import axios from 'axios';
import { ScheduleRequest, ScheduleResponse } from '../types';

const API_BASE = '/api/schedules';

export async function fetchSchedules(): Promise<ScheduleResponse[]> {
  const response = await axios.get<ScheduleResponse[]>(API_BASE);
  return response.data;
}

export async function fetchSchedule(id: string): Promise<ScheduleResponse> {
  const response = await axios.get<ScheduleResponse>(`${API_BASE}/${id}`);
  return response.data;
}

export async function createSchedule(request: ScheduleRequest): Promise<ScheduleResponse> {
  const response = await axios.post<ScheduleResponse>(API_BASE, request);
  return response.data;
}

export async function updateSchedule(id: string, request: ScheduleRequest): Promise<ScheduleResponse> {
  const response = await axios.put<ScheduleResponse>(`${API_BASE}/${id}`, request);
  return response.data;
}

export async function deleteSchedule(id: string): Promise<void> {
  await axios.delete(`${API_BASE}/${id}`);
}
