import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'

import { ApiError, api } from '../api'
import DaySheet from '../components/DaySheet'
import EventForm from '../components/EventForm'
import MonthGrid from '../components/MonthGrid'
import SettingsSheet from '../components/SettingsSheet'
import { copyText, eventsOn, monthDays, prefs, ymd } from '../lib'
import type { CalEvent, CalendarInfo, EventInput } from '../types'

const REFRESH_MS = 30_000

type FormState = { event: CalEvent | null; date: string } | null

export default function CalendarPage() {
  const { code = '' } = useParams()
  const navigate = useNavigate()

  const [cal, setCal] = useState<CalendarInfo | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [me, setMe] = useState<'A' | 'B' | null>(() => prefs.me(code))
  const [cursor, setCursor] = useState(() => {
    const now = new Date()
    return { year: now.getFullYear(), month: now.getMonth() }
  })
  const [events, setEvents] = useState<CalEvent[]>([])
  const [selectedDay, setSelectedDay] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(null)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [toast, setToast] = useState<string | null>(null)

  const range = useMemo(() => {
    const days = monthDays(cursor.year, cursor.month)
    return { from: ymd(days[0]), to: ymd(days[days.length - 1]) }
  }, [cursor])

  // 달력 정보
  useEffect(() => {
    let alive = true
    api
      .getCalendar(code)
      .then((c) => {
        if (!alive) return
        setCal(c)
        prefs.setLastCode(c.code)
      })
      .catch((e) => alive && e instanceof ApiError && e.status === 404 && setNotFound(true))
    return () => {
      alive = false
    }
  }, [code])

  // 이번 달 일정 (+ 30초마다, 창으로 돌아올 때 새로고침해서 상대방 변경 반영)
  const loadEvents = useCallback(async () => {
    try {
      setEvents(await api.listEvents(code, range.from, range.to))
    } catch {
      /* 다음 새로고침 때 다시 시도 */
    }
  }, [code, range])

  useEffect(() => {
    loadEvents()
    const timer = window.setInterval(loadEvents, REFRESH_MS)
    const onFocus = () => document.visibilityState === 'visible' && loadEvents()
    document.addEventListener('visibilitychange', onFocus)
    window.addEventListener('focus', onFocus)
    return () => {
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', onFocus)
      window.removeEventListener('focus', onFocus)
    }
  }, [loadEvents])

  useEffect(() => {
    if (!toast) return
    const t = window.setTimeout(() => setToast(null), 2000)
    return () => window.clearTimeout(t)
  }, [toast])

  const moveMonth = (delta: number) =>
    setCursor(({ year, month }) => {
      const d = new Date(year, month + delta, 1)
      return { year: d.getFullYear(), month: d.getMonth() }
    })

  const goToday = () => {
    const now = new Date()
    setCursor({ year: now.getFullYear(), month: now.getMonth() })
  }

  const chooseMe = (v: 'A' | 'B') => {
    setMe(v)
    prefs.setMe(code, v)
  }

  async function shareLink() {
    const ok = await copyText(window.location.href)
    setToast(ok ? '링크를 복사했어요. 상대에게 보내 주세요!' : '주소창의 링크를 직접 복사해 주세요.')
  }

  async function saveEvent(input: EventInput) {
    if (!form) return
    if (form.event) await api.updateEvent(code, form.event.id, input)
    else await api.createEvent(code, input)
    setForm(null)
    setSelectedDay(input.startDate)
    await loadEvents()
  }

  async function deleteEvent(e: CalEvent) {
    if (!window.confirm(`'${e.title}' 일정을 삭제할까요?`)) return
    try {
      await api.deleteEvent(code, e.id)
      await loadEvents()
    } catch (err) {
      setToast(err instanceof Error ? err.message : '삭제하지 못했어요.')
    }
  }

  async function saveSettings(a: string, b: string, mine: 'A' | 'B' | null) {
    setCal(await api.renameMembers(code, a, b))
    if (mine) chooseMe(mine)
    setSettingsOpen(false)
  }

  if (notFound) {
    return (
      <div className="center-screen">
        <p>달력을 찾을 수 없어요. 링크를 다시 확인해 주세요.</p>
        <button className="btn primary" onClick={() => navigate('/', { replace: true })}>
          내 달력으로 가기
        </button>
      </div>
    )
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo">CC</span>
          <span className="tagline">우리 CC할까?</span>
        </div>
        <div className="top-actions">
          <button className="btn ghost sm" onClick={shareLink}>
            링크 공유
          </button>
          <button className="icon-btn" onClick={() => setSettingsOpen(true)} aria-label="설정" disabled={!cal}>
            ⚙
          </button>
        </div>
      </header>

      {cal && !me && (
        <div className="whoami">
          <span>이 기기에서 누구세요?</span>
          <button className="pill" style={{ ['--c' as string]: 'var(--owner-a)' }} onClick={() => chooseMe('A')}>
            {cal.memberAName}
          </button>
          <button className="pill" style={{ ['--c' as string]: 'var(--owner-b)' }} onClick={() => chooseMe('B')}>
            {cal.memberBName}
          </button>
        </div>
      )}

      <main className="main">
        <div className="month-bar">
          <button className="icon-btn" onClick={() => moveMonth(-1)} aria-label="이전 달">
            ‹
          </button>
          <h1>
            {cursor.year}년 {cursor.month + 1}월
          </h1>
          <button className="icon-btn" onClick={() => moveMonth(1)} aria-label="다음 달">
            ›
          </button>
          <button className="btn ghost sm today-btn" onClick={goToday}>
            오늘
          </button>
        </div>

        {cal && (
          <div className="legend">
            <span>
              <i style={{ background: 'var(--owner-a)' }} />
              {cal.memberAName}
            </span>
            <span>
              <i style={{ background: 'var(--owner-b)' }} />
              {cal.memberBName}
            </span>
            <span>
              <i style={{ background: 'var(--owner-both)' }} />
              같이
            </span>
          </div>
        )}

        <MonthGrid year={cursor.year} month={cursor.month} events={events} onPickDay={setSelectedDay} />
      </main>

      {cal && selectedDay && !form && (
        <DaySheet
          day={selectedDay}
          cal={cal}
          events={eventsOn(events, selectedDay)}
          onClose={() => setSelectedDay(null)}
          onAdd={() => setForm({ event: null, date: selectedDay })}
          onEdit={(e) => setForm({ event: e, date: selectedDay })}
          onDelete={deleteEvent}
        />
      )}

      {cal && form && (
        <EventForm
          cal={cal}
          event={form.event}
          defaultDate={form.date}
          defaultOwner={me ?? 'BOTH'}
          onClose={() => setForm(null)}
          onSubmit={saveEvent}
        />
      )}

      {cal && settingsOpen && (
        <SettingsSheet cal={cal} me={me} onClose={() => setSettingsOpen(false)} onSave={saveSettings} />
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}
