/** A = 첫 번째 사람, B = 두 번째 사람, BOTH = 같이 */
export type Owner = 'A' | 'B' | 'BOTH'

export interface CalendarInfo {
  code: string
  memberAName: string
  memberBName: string
}

export interface CalEvent {
  id: number
  title: string
  startDate: string // YYYY-MM-DD
  endDate: string
  startTime: string | null // HH:mm(:ss), null = 하루 종일
  endTime: string | null
  location: string | null
  memo: string | null
  owner: Owner
}

export type EventInput = Omit<CalEvent, 'id'>
