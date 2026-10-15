import client from '../../api/client';

/** 응답: { wishlisted, count }. 비로그인이면 wishlisted는 항상 false. */
export function fetchWishlistStatus(itemId) {
  return client.get(`/items/${itemId}/wishlist`);
}

/** 이미 찜했어도 에러 없이 { wishlisted: true, count } (연타해도 한 번만 저장). */
export function addWishlist(itemId) {
  return client.post(`/items/${itemId}/wishlist`);
}

export function removeWishlist(itemId) {
  return client.delete(`/items/${itemId}/wishlist`);
}

/** 내 찜 목록 (최근에 찜한 순, 페이지). content는 상품 목록과 같은 모양. */
export function fetchMyWishlist(params) {
  return client.get('/members/me/wishlist', { params });
}
