import { useEffect, useState, type FormEvent } from 'react'

import Modal from './Modal'
import { api } from '../api'
import { prefs } from '../lib'
import type { CalendarInfo, KakaoStatus, Person } from '../types'

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
  const [kakao, setKakao] = useState<KakaoStatus | null>(null)

  useEffect(() => {
    api.kakaoStatus(cal.code).then(setKakao, () => setKakao({ available: false, a: false, b: false }))
  }, [cal.code])

  function connect(v: Person) {
    prefs.setMe(cal.code, v) // 카카오 로그인 갔다 와도 "나는 누구"가 유지되게
    window.location.href = api.kakaoConnectUrl(cal.code, v)
  }

  async function disconnect(v: Person) {
    const name = v === 'A' ? a : b
    if (!window.confirm(`${name}의 카톡 알림 연결을 끊을까요?`)) return
    try {
      await api.kakaoDisconnect(cal.code, v)
      setKakao(await api.kakaoStatus(cal.code))
    } catch (err) {
      setError(err instanceof Error ? err.message : '연결을 끊지 못했어요.')
    }
  }

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

        <div className="field">
          <span>카톡 알림</span>
          <p className="hint">상대가 일정을 추가·수정·삭제하면 카톡 "나와의 채팅"으로 알려 줘요. "같이" 일정은 둘 다 받아요.</p>
          {!kakao ? (
            <p className="hint">불러오는 중…</p>
          ) : !kakao.available ? (
            <p className="hint">서버에 카카오 키가 설정되지 않아서 지금은 쓸 수 없어요.</p>
          ) : (
            <ul className="kakao-list">
              {(['A', 'B'] as const).map((v) => {
                const connected = v === 'A' ? kakao.a : kakao.b
                return (
                  <li key={v}>
                    <span className="swatch-label">
                      <i style={{ background: `var(--owner-${v.toLowerCase()})` }} />
                      {v === 'A' ? a || '첫 번째' : b || '두 번째'}
                    </span>
                    <span className={`kakao-state ${connected ? 'on' : ''}`}>{connected ? '연결됨' : '연결 안 됨'}</span>
                    {connected ? (
                      <button type="button" className="btn ghost sm" onClick={() => disconnect(v)}>
                        끊기
                      </button>
                    ) : mine === v ? (
                      <button type="button" className="btn kakao sm" onClick={() => connect(v)}>
                        카톡 연결
                      </button>
                    ) : null}
                  </li>
                )
              })}
            </ul>
          )}
          {kakao?.available && !mine && <p className="hint">먼저 위에서 "이 기기에서 나는"을 골라 주세요.</p>}
        </div>

        {error && <p className="form-error">{error}</p>}
      </form>
    </Modal>
  )
}
