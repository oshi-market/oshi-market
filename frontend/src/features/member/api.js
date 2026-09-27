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
