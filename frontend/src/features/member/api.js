import client from '../../api/client';

export function signup({ email, password, nickname }) {
  return client.post('/auth/signup', { email, password, nickname });
}

export function login({ email, password }) {
  return client.post('/auth/login', { email, password });
}

export function fetchMe() {
  return client.get('/members/me');
}

export function updateMe({ nickname }) {
  return client.patch('/members/me', { nickname });
}

/** 다른 회원의 공개 프로필 (닉네임·가입일). 로그인 없이 조회 가능. */
export function fetchMemberProfile(memberId) {
  return client.get(`/members/${memberId}`);
}
