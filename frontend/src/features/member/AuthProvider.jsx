import { useCallback, useEffect, useState } from 'react';
import { fetchMe, login as loginApi, signup as signupApi } from './api';
import { AuthContext } from './authContext';

const TOKEN_KEY = 'accessToken';

/**
 * 로그인 상태(accessToken/내 정보)를 앱 전역에서 관리.
 * accessToken은 axios 인터셉터(src/api/client.js)가 localStorage에서 직접 읽어서
 * Authorization 헤더에 붙이므로, 여기서는 localStorage 동기화 + 내 정보 캐싱만 담당.
 */
export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [member, setMember] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const loadMe = useCallback(async () => {
    try {
      const { data } = await fetchMe();
      setMember(data);
    } catch {
      // 토큰이 만료/무효면 로그아웃 상태로 되돌린다.
      localStorage.removeItem(TOKEN_KEY);
      setAccessToken(null);
      setMember(null);
    }
  }, []);

  useEffect(() => {
    if (!accessToken) {
      setIsLoading(false);
      return;
    }
    loadMe().finally(() => setIsLoading(false));
    // 최초 마운트 시 1회만 — accessToken은 login()/logout()이 상태와 함께 관리한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function login(credentials) {
    const { data } = await loginApi(credentials);
    localStorage.setItem(TOKEN_KEY, data.accessToken);
    setAccessToken(data.accessToken);
    await loadMe();
  }

  async function signup(form) {
    await signupApi(form);
    // 회원가입 API는 토큰을 안 내려주므로, 가입 직후 같은 자격증명으로 로그인까지 이어서 처리.
    await login({ email: form.email, password: form.password });
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    setAccessToken(null);
    setMember(null);
  }

  const value = {
    member,
    isAuthenticated: Boolean(accessToken),
    isLoading,
    login,
    signup,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
