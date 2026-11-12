import type { CalEvent, CalendarInfo, Owner } from './types'

export const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

const pad = (n: number) => String(n).padStart(2, '0')

/** Date → 'YYYY-MM-DD' (로컬 시간 기준) */
export function ymd(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function parseYmd(s: string): Date {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function addDays(d: Date, n: number): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + n)
}

/** 달력에 보일 날짜들 (앞뒤 달 포함, 일요일 시작, 주 단위) */
export function monthDays(year: number, month: number): Date[] {
  const first = new Date(year, month, 1)
  const start = addDays(first, -first.getDay())
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const weeks = Math.ceil((first.getDay() + daysInMonth) / 7)
  return Array.from({ length: weeks * 7 }, (_, i) => addDays(start, i))
}

/** 'HH:mm:ss' → 'HH:mm' */
export const hm = (t: string | null) => (t ? t.slice(0, 5) : '')

export function timeLabel(e: CalEvent): string {
  if (!e.startTime) return '하루 종일'
  return e.endTime ? `${hm(e.startTime)} – ${hm(e.endTime)}` : hm(e.startTime)
}

export function dayLabel(s: string): string {
  const d = parseYmd(s)
  return `${d.getMonth() + 1}월 ${d.getDate()}일 (${WEEKDAYS[d.getDay()]})`
}

export function periodLabel(e: CalEvent): string | null {
  if (e.startDate === e.endDate) return null
  const a = parseYmd(e.startDate)
  const b = parseYmd(e.endDate)
  return `${a.getMonth() + 1}/${a.getDate()} – ${b.getMonth() + 1}/${b.getDate()}`
}

export const OWNER_COLOR: Record<Owner, string> = {
  A: 'var(--owner-a)',
  B: 'var(--owner-b)',
  BOTH: 'var(--owner-both)',
}

export function ownerName(cal: CalendarInfo, owner: Owner): string {
  if (owner === 'A') return cal.memberAName
  if (owner === 'B') return cal.memberBName
  return '같이'
}

/** 하루 종일·여러 날 일정 먼저, 그다음 시간순 */
export function sortEvents(list: CalEvent[]): CalEvent[] {
  return [...list].sort((x, y) => {
    const xs = x.startTime ?? ''
    const ys = y.startTime ?? ''
    if (xs !== ys) return xs < ys ? -1 : 1
    return x.id - y.id
  })
}

export function eventsOn(events: CalEvent[], day: string): CalEvent[] {
  return sortEvents(events.filter((e) => e.startDate <= day && day <= e.endDate))
}

/* ---------- 브라우저에만 저장하는 개인 설정 ---------- */

const LAST_CODE = 'cc:lastCode'
const meKey = (code: string) => `cc:me:${code}`

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}
function safeSet(key: string, value: string) {
  try {
    localStorage.setItem(key, value)
  } catch {
    /* 저장 불가(사생활 보호 모드 등) — 무시 */
  }
}

export const prefs = {
  lastCode: () => safeGet(LAST_CODE),
  setLastCode: (code: string) => safeSet(LAST_CODE, code),
  me: (code: string): 'A' | 'B' | null => {
    const v = safeGet(meKey(code))
    return v === 'A' || v === 'B' ? v : null
  },
  setMe: (code: string, me: 'A' | 'B') => safeSet(meKey(code), me),
}

/** http(IP 주소)에서는 navigator.clipboard 가 막혀 있어서 예전 방식으로 대체 */
export async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    /* 아래 방식으로 재시도 */
  }
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  let ok = false
  try {
    ok = document.execCommand('copy')
  } catch {
    ok = false
  }
  document.body.removeChild(ta)
  return ok
}
