import Modal from './Modal'
import { OWNER_COLOR, dayLabel, ownerName, periodLabel, timeLabel } from '../lib'
import type { CalEvent, CalendarInfo } from '../types'

interface Props {
  day: string
  cal: CalendarInfo
  events: CalEvent[]
  onClose: () => void
  onAdd: () => void
  onEdit: (e: CalEvent) => void
  onDelete: (e: CalEvent) => void
}

/** 날짜를 눌렀을 때: 그날 일정을 자세히 */
export default function DaySheet({ day, cal, events, onClose, onAdd, onEdit, onDelete }: Props) {
  return (
    <Modal
      title={dayLabel(day)}
      onClose={onClose}
      footer={
        <button className="btn primary full" onClick={onAdd}>
          + 일정 추가
        </button>
      }
    >
      {events.length === 0 ? (
        <p className="empty">아직 일정이 없어요.</p>
      ) : (
        <ul className="event-list">
          {events.map((e) => {
            const period = periodLabel(e)
            return (
              <li key={e.id} className="event-card" style={{ ['--c' as string]: OWNER_COLOR[e.owner] }}>
                <div className="event-top">
                  <span className="owner-tag">{ownerName(cal, e.owner)}</span>
                  <span className="event-time">
                    {timeLabel(e)}
                    {period && ` · ${period}`}
                  </span>
                </div>
                <h3 className="event-title">{e.title}</h3>
                {e.location && <p className="event-loc">📍 {e.location}</p>}
                {e.memo && <p className="event-memo">{e.memo}</p>}
                <div className="event-actions">
                  <button className="btn ghost sm" onClick={() => onEdit(e)}>
                    수정
                  </button>
                  <button className="btn ghost sm danger" onClick={() => onDelete(e)}>
                    삭제
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </Modal>
  )
}
