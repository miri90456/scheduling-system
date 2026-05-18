import axios from 'axios';
import { TaskResponse } from '../types';

const API_BASE = '/api/tasks';

export async function fetchTasks(): Promise<TaskResponse[]> {
  const response = await axios.get<TaskResponse[]>(API_BASE);
  return response.data;
}
