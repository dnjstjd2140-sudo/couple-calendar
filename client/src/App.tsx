import { useEffect, useRef, useState } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'

import { ApiError, api } from './api'
import CalendarPage from './pages/CalendarPage'
import { prefs } from './lib'

/** 첫 화면: 내 달력이 있으면 그 달력으로, 없으면 새로 만들어서 바로 달력으로 */
function Home() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const started = useRef(false)

  useEffect(() => {
    if (started.current) return
    started.current = true
    ;(async () => {
      try {
        const last = prefs.lastCode()
        if (last) {
          try {
            await api.getCalendar(last)
            navigate(`/c/${last}`, { replace: true })
            return
          } catch (e) {
            if (!(e instanceof ApiError && e.status === 404)) throw e
          }
        }
        const created = await api.createCalendar()
        prefs.setLastCode(created.code)
        navigate(`/c/${created.code}`, { replace: true })
      } catch (e) {
        setError(e instanceof Error ? e.message : '달력을 열지 못했어요.')
      }
    })()
  }, [navigate])

  return <div className="center-screen">{error ?? '달력을 여는 중…'}</div>
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/c/:code" element={<CalendarPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
