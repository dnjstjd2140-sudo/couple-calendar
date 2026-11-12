import { useMemo } from 'react'

import { OWNER_COLOR, WEEKDAYS, eventsOn, hm, monthDays, ymd } from '../lib'
import type { CalEvent } from '../types'

const MAX_CHIPS = 3

interface Props {
  year: number
  month: number
  events: CalEvent[]
  onPickDay: (day: string) => void
}

/** 날짜 칸에는 제목만 짧게. 칸을 누르면 그날 일정이 자세히 열린다. */
export default function MonthGrid({ year, month, events, onPickDay }: Props) {
  const days = useMemo(() => monthDays(year, month), [year, month])
  const today = ymd(new Date())

  return (
    <div className="grid">
      {WEEKDAYS.map((w, i) => (
        <div key={w} className={`weekday ${i === 0 ? 'sun' : i === 6 ? 'sat' : ''}`}>
          {w}
        </div>
      ))}
      {days.map((d) => {
        const key = ymd(d)
        const list = eventsOn(events, key)
        const outside = d.getMonth() !== month
        const dow = d.getDay()
        return (
          <button
            key={key}
            className={`cell ${outside ? 'outside' : ''} ${key === today ? 'today' : ''}`}
            onClick={() => onPickDay(key)}
            aria-label={`${d.getMonth() + 1}월 ${d.getDate()}일, 일정 ${list.length}개`}
          >
            <span className={`daynum ${dow === 0 ? 'sun' : dow === 6 ? 'sat' : ''}`}>{d.getDate()}</span>
            <span className="chips">
              {list.slice(0, MAX_CHIPS).map((e) => {
                const multi = e.startDate !== e.endDate
                return (
                  <span
                    key={e.id}
                    className={`chip ${multi ? 'multi' : ''}`}
                    style={{ ['--c' as string]: OWNER_COLOR[e.owner] }}
                  >
                    {e.startTime && !multi && <span className="chip-time">{hm(e.startTime)}</span>}
                    <span className="chip-title">{e.title}</span>
                  </span>
                )
              })}
              {list.length > MAX_CHIPS && <span className="more">+{list.length - MAX_CHIPS}</span>}
            </span>
          </button>
        )
      })}
    </div>
  )
}
