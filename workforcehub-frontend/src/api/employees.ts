import client from './axiosClient'
import type { Employee, Page, PageParams } from '@/types'

export async function fetchEmployees(params: PageParams & { status?: string }): Promise<Page<Employee>> {
  const { data } = await client.get<Page<Employee>>('/api/v1/employees', { params })
  return data
}

export async function fetchEmployee(id: string): Promise<Employee> {
  const { data } = await client.get<Employee>(`/api/v1/employees/${id}`)
  return data
}
