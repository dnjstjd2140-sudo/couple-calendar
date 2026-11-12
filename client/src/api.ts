import type { CalEvent, CalendarInfo, EventInput } from './types'

const BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message)
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!res.ok) {
    let message = '요청에 실패했어요. 잠시 후 다시 시도해 주세요.'
    try {
      const body = await res.json()
      if (body?.message) message = body.message
    } catch {
      /* 본문 없음 */
    }
    throw new ApiError(res.status, message)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}

export const api = {
  createCalendar: () => request<CalendarInfo>('/calendars', { method: 'POST' }),
  getCalendar: (code: string) => request<CalendarInfo>(`/calendars/${code}`),
  renameMembers: (code: string, memberAName: string, memberBName: string) =>
    request<CalendarInfo>(`/calendars/${code}`, {
      method: 'PATCH',
      body: JSON.stringify({ memberAName, memberBName }),
    }),

  listEvents: (code: string, from: string, to: string) =>
    request<CalEvent[]>(`/calendars/${code}/events?from=${from}&to=${to}`),
  createEvent: (code: string, input: EventInput) =>
    request<CalEvent>(`/calendars/${code}/events`, { method: 'POST', body: JSON.stringify(input) }),
  updateEvent: (code: string, id: number, input: EventInput) =>
    request<CalEvent>(`/calendars/${code}/events/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  deleteEvent: (code: string, id: number) =>
    request<void>(`/calendars/${code}/events/${id}`, { method: 'DELETE' }),
}
