import client from './axiosClient'

export interface MeResponse {
  id: string
  tenantId: string
  role: string
  email: string
}

export async function fetchMe(): Promise<MeResponse> {
  const { data } = await client.get<MeResponse>('/auth/me')
  return data
}

