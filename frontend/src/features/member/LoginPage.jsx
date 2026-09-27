import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from './useAuth';

function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);
    try {
      await login(form);
      navigate('/');
    } catch (err) {
      setError(getErrorMessage(err, '로그인에 실패했습니다.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="card">
        <h2>로그인</h2>
        <form onSubmit={handleSubmit}>
          <label className="field">
            이메일
            <input
              className="input"
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              autoComplete="email"
              required
            />
          </label>
          <label className="field">
            비밀번호
            <input
              className="input"
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              autoComplete="current-password"
              required
            />
          </label>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <p className="switch-link">
          계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>
      </div>
    </div>
  );
}

export default LoginPage;
