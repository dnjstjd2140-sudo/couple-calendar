import { useState, type FormEvent } from 'react'

import Modal from './Modal'
import { hm } from '../lib'
import type { CalEvent, CalendarInfo, EventInput, Owner } from '../types'

interface Props {
  cal: CalendarInfo
  /** 수정이면 기존 일정, 추가면 null */
  event: CalEvent | null
  defaultDate: string
  defaultOwner: Owner
  onClose: () => void
  onSubmit: (input: EventInput) => Promise<void>
}

export default function EventForm({ cal, event, defaultDate, defaultOwner, onClose, onSubmit }: Props) {
  const [title, setTitle] = useState(event?.title ?? '')
  const [owner, setOwner] = useState<Owner>(event?.owner ?? defaultOwner)
  const [startDate, setStartDate] = useState(event?.startDate ?? defaultDate)
  const [endDate, setEndDate] = useState(event?.endDate ?? defaultDate)
  const [allDay, setAllDay] = useState(event ? !event.startTime : true)
  const [startTime, setStartTime] = useState(hm(event?.startTime ?? null) || '12:00')
  const [endTime, setEndTime] = useState(hm(event?.endTime ?? null))
  const [location, setLocation] = useState(event?.location ?? '')
  const [memo, setMemo] = useState(event?.memo ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const owners: { value: Owner; label: string }[] = [
    { value: 'A', label: cal.memberAName },
    { value: 'B', label: cal.memberBName },
    { value: 'BOTH', label: '같이' },
  ]

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!title.trim()) return setError('제목을 적어 주세요.')
    if (endDate < startDate) return setError('끝나는 날이 시작하는 날보다 빨라요.')
    if (!allDay && endTime && startDate === endDate && endTime < startTime)
      return setError('끝나는 시간이 시작 시간보다 빨라요.')
    setSaving(true)
    setError(null)
    try {
      await onSubmit({
        title: title.trim(),
        owner,
        startDate,
        endDate,
        startTime: allDay ? null : startTime,
        endTime: allDay || !endTime ? null : endTime,
        location: location.trim() || null,
        memo: memo.trim() || null,
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : '저장하지 못했어요.')
      setSaving(false)
    }
  }

  return (
    <Modal
      title={event ? '일정 수정' : '새 일정'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn ghost" onClick={onClose}>
            취소
          </button>
          <button type="submit" form="event-form" className="btn primary" disabled={saving}>
            {saving ? '저장 중…' : '저장'}
          </button>
        </>
      }
    >
      <form id="event-form" className="form" onSubmit={submit}>
        <label className="field">
          <span>제목</span>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="예) 성수 데이트"
            maxLength={100}
            autoFocus
          />
        </label>

        <div className="field">
          <span>누구 일정?</span>
          <div className="segmented">
            {owners.map((o) => (
              <button
                key={o.value}
                type="button"
                className={owner === o.value ? 'on' : ''}
                style={{ ['--c' as string]: `var(--owner-${o.value === 'BOTH' ? 'both' : o.value.toLowerCase()})` }}
                onClick={() => setOwner(o.value)}
              >
                {o.label}
              </button>
            ))}
          </div>
        </div>

        <div className="row">
          <label className="field">
            <span>시작일</span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value)
                if (endDate < e.target.value) setEndDate(e.target.value)
              }}
              required
            />
          </label>
          <label className="field">
            <span>종료일</span>
            <input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} required />
          </label>
        </div>

        <label className="check">
          <input type="checkbox" checked={allDay} onChange={(e) => setAllDay(e.target.checked)} />
          하루 종일
        </label>

        {!allDay && (
          <div className="row">
            <label className="field">
              <span>시작 시간</span>
              <input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} required />
            </label>
            <label className="field">
              <span>끝 시간 (선택)</span>
              <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
            </label>
          </div>
        )}

        <label className="field">
          <span>장소 (선택)</span>
          <input value={location} onChange={(e) => setLocation(e.target.value)} maxLength={100} />
        </label>

        <label className="field">
          <span>메모 (선택)</span>
          <textarea value={memo} onChange={(e) => setMemo(e.target.value)} rows={3} maxLength={2000} />
        </label>

        {error && <p className="form-error">{error}</p>}
      </form>
    </Modal>
  )
}
