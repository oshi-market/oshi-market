import client from '../../api/client';

export function fetchCurations(params) {
  return client.get('/curations', { params });
}

export function fetchCuration(curationId) {
  return client.get(`/curations/${curationId}`);
}
