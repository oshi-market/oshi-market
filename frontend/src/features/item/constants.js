export const CONDITION_LABEL = {
  NEW: '새 상품',
  USED: '중고',
};

export const STATUS_LABEL = {
  SELLING: '판매중',
  IN_TRANSACTION: '거래중',
  SOLD_OUT: '판매완료',
};

/** 백엔드 ItemCategory enum과 1:1. 순서가 등록 화면 드롭다운 순서. */
export const CATEGORY_LABEL = {
  FIGURE: '피규어',
  ACRYLIC: '아크릴 스탠드',
  KEYRING: '키링',
  CAN_BADGE: '캔뱃지',
  PLUSH: '인형·누이',
  PHOTO_CARD: '포토카드·일러스트 카드',
  TRADING_CARD: '트레이딩 카드(TCG)',
  POSTER: '포스터·태피스트리',
  FABRIC: '쿠션·패브릭',
  MEDIA: '음반·블루레이·도서',
  ETC: '기타',
};

/** 상품 목록 정렬. 주소창에는 key(latest 등)만 저장하고, 서버에는 sort 값으로 보낸다(서버 허용 필드: createdAt, price). */
export const SORT_OPTIONS = {
  latest: { label: '최신순', sort: 'createdAt,desc' },
  price_asc: { label: '가격 낮은순', sort: 'price,asc' },
  price_desc: { label: '가격 높은순', sort: 'price,desc' },
};
