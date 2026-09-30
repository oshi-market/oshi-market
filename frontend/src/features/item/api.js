import client from '../../api/client';

export function fetchItems(params) {
  return client.get('/items', { params });
}

export function fetchItem(itemId) {
  return client.get(`/items/${itemId}`);
}

export function createItem(data) {
  return client.post('/items', data);
}

export function updateItem(itemId, data) {
  return client.patch(`/items/${itemId}`, data);
}

export function deleteItem(itemId) {
  return client.delete(`/items/${itemId}`);
}
