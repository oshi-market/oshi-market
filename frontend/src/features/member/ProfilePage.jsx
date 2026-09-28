import { useEffect, useState } from 'react';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from './useAuth';

function formatDate(isoString) {
  if (!isoString) return '-';
  return isoString.slice(0, 10);
}

function ProfilePage() {
  const { member, updateProfile } = useAuth();
  const [nickname, setNickname] = useState(member?.nickname ?? '');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setNickname(member?.nickname ?? '');
  }, [member?.nickname]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSuccessMessage('');
    setIsSubmitting(true);
    try {
      await updateProfile({ nickname });
      setSuccessMessage('프로필이 수정되었습니다.');
    } catch (err) {
      setError(getErrorMessage(err, '프로필 수정에 실패했습니다.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!member) {
    return (
      <div className="page">
        <p className="muted">불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="page profile-page">
      <div className="card">
        <h2>프로필</h2>

        <div className="profile-readonly">
          <div className="field">
            이메일
            <p>{member.email}</p>
          </div>
          <div className="field">
            신뢰도
            <p>{member.trustScore}점</p>
          </div>
          <div className="field">
            가입일
            <p>{formatDate(member.createdAt)}</p>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          <label className="field">
            닉네임
            <input
              className="input"
              name="nickname"
              value={nickname}
              onChange={(event) => setNickname(event.target.value)}
              minLength={2}
              maxLength={20}
              required
            />
          </label>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          {successMessage && <p className="form-success">{successMessage}</p>}
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? '저장 중...' : '저장하기'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default ProfilePage;
