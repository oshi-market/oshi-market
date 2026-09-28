/**
 * 홈 상품 그리드용 임시 데이터. 백엔드 상품 목록 API(GET /api/items) 연동 시 제거한다.
 * 필드명은 ItemResponse(id/title/price/status)에 맞춰 두었고, thumbnailUrl은 이미지 업로드 기능 전까지의 가정값.
 */
const mockItems = [
  { id: 1, title: '귀멸의 칼날 탄지로 아크릴 스탠드', price: 18000, status: 'SELLING', thumbnailUrl: 'https://picsum.photos/seed/oshi1/400/400' },
  { id: 2, title: '주술회전 고죠 사토루 캔뱃지 세트', price: 12000, status: 'IN_TRANSACTION', thumbnailUrl: 'https://picsum.photos/seed/oshi2/400/400' },
  { id: 3, title: '원신 라이덴 쇼군 1/7 스케일 피규어', price: 165000, status: 'SELLING', thumbnailUrl: 'https://picsum.photos/seed/oshi3/400/400' },
  { id: 4, title: '블루아카이브 호시노 누이 인형', price: 35000, status: 'SOLD_OUT', thumbnailUrl: 'https://picsum.photos/seed/oshi4/400/400' },
  { id: 5, title: '하이큐!! 카라스노 유니폼 키링', price: 9000, status: 'SELLING', thumbnailUrl: 'https://picsum.photos/seed/oshi5/400/400' },
  { id: 6, title: '체인소맨 포치타 쿠션 (미개봉)', price: 27000, status: 'SELLING', thumbnailUrl: 'https://picsum.photos/seed/oshi6/400/400' },
  { id: 7, title: '봇치 더 록! 결속밴드 포스터 세트', price: 22000, status: 'IN_TRANSACTION', thumbnailUrl: 'https://picsum.photos/seed/oshi7/400/400' },
  { id: 8, title: '프로젝트 세카이 미쿠 한정 포토카드', price: 45000, status: 'SOLD_OUT', thumbnailUrl: 'https://picsum.photos/seed/oshi8/400/400' },
];

export default mockItems;
