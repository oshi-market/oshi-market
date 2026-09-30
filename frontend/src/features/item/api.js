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

/** 사진 여러 장 업로드 (multipart, 파트 이름 files). 응답은 해당 상품의 전체 사진 목록. */
export function uploadItemImages(itemId, files) {
  const formData = new FormData();
  files.forEach((file) => formData.append('files', file));
  return client.post(`/items/${itemId}/images`, formData);
}

export function deleteItemImage(itemId, imageId) {
  return client.delete(`/items/${itemId}/images/${imageId}`);
}

/** 작품 마스터 목록 (작품명 드롭다운용). */
export function fetchWorks() {
  return client.get('/works');
}
