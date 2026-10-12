import axios from 'axios';

const TOKEN_KEY = 'accessToken';

/** 토큰이 만료/무효라 서버가 401을 돌려줬을 때 발생시키는 이벤트. 로그인 상태 정리는 AuthProvider가 담당. */
export const AUTH_EXPIRED_EVENT = 'auth:expired';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    // 토큰을 붙여 보낸 요청이 401이면 토큰 만료/무효. 로그인 실패처럼 토큰 없이 보낸 요청의 401은 제외하고,
    // 다시 로그인한 뒤에 늦게 도착한 옛 토큰 요청의 401이 새 로그인을 지우지 않도록 현재 토큰과 같을 때만 처리
    const sentToken = error.config?.headers?.Authorization?.replace('Bearer ', '');
    if (error.response?.status === 401 && sentToken && sentToken === localStorage.getItem(TOKEN_KEY)) {
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
    }
    return Promise.reject(error);
  },
);

export default client;
