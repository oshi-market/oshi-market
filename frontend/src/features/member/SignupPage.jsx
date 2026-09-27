import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from './useAuth';

function SignupPage() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '', nickname: '' });
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
      await signup(form);
      navigate('/');
    } catch (err) {
      setError(getErrorMessage(err, '회원가입에 실패했습니다.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <h2>회원가입</h2>
      <form onSubmit={handleSubmit}>
        <label>
          이메일
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            autoComplete="email"
            required
          />
        </label>
        <label>
          비밀번호
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={handleChange}
            autoComplete="new-password"
            minLength={8}
            maxLength={64}
            required
          />
        </label>
        <label>
          닉네임
          <input
            type="text"
            name="nickname"
            value={form.nickname}
            onChange={handleChange}
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
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? '가입 중...' : '회원가입'}
        </button>
      </form>
      <p>
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </div>
  );
}

export default SignupPage;
