import { useState, type FormEvent } from 'react'

import Modal from './Modal'
import type { CalendarInfo } from '../types'

interface Props {
  cal: CalendarInfo
  me: 'A' | 'B' | null
  onClose: () => void
  onSave: (a: string, b: string, me: 'A' | 'B' | null) => Promise<void>
}

export default function SettingsSheet({ cal, me, onClose, onSave }: Props) {
  const [a, setA] = useState(cal.memberAName)
  const [b, setB] = useState(cal.memberBName)
  const [mine, setMine] = useState(me)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!a.trim() || !b.trim()) return setError('이름을 둘 다 적어 주세요.')
    setSaving(true)
    try {
      await onSave(a.trim(), b.trim(), mine)
    } catch (err) {
      setError(err instanceof Error ? err.message : '저장하지 못했어요.')
      setSaving(false)
    }
  }

  return (
    <Modal
      title="설정"
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn ghost" onClick={onClose}>
            취소
          </button>
          <button type="submit" form="settings-form" className="btn primary" disabled={saving}>
            저장
          </button>
        </>
      }
    >
      <form id="settings-form" className="form" onSubmit={submit}>
        <p className="hint">이름은 둘 다에게 똑같이 보여요.</p>
        <div className="row">
          <label className="field">
            <span className="swatch-label">
              <i style={{ background: 'var(--owner-a)' }} /> 첫 번째 사람
            </span>
            <input value={a} onChange={(e) => setA(e.target.value)} maxLength={20} />
          </label>
          <label className="field">
            <span className="swatch-label">
              <i style={{ background: 'var(--owner-b)' }} /> 두 번째 사람
            </span>
            <input value={b} onChange={(e) => setB(e.target.value)} maxLength={20} />
          </label>
        </div>

        <div className="field">
          <span>이 기기에서 나는</span>
          <div className="segmented">
            {(['A', 'B'] as const).map((v) => (
              <button
                key={v}
                type="button"
                className={mine === v ? 'on' : ''}
                style={{ ['--c' as string]: `var(--owner-${v.toLowerCase()})` }}
                onClick={() => setMine(v)}
              >
                {v === 'A' ? a || '첫 번째' : b || '두 번째'}
              </button>
            ))}
          </div>
          <p className="hint">새 일정을 만들 때 기본으로 내 이름이 선택돼요.</p>
        </div>

        {error && <p className="form-error">{error}</p>}
      </form>
    </Modal>
  )
}
