export const TRANSACTION_STATUS_LABEL = {
  REQUESTED: '거래요청',
  IN_PROGRESS: '거래중',
  COMPLETED: '거래완료',
  CANCELLED: '거래취소',
};

export const ROLE_LABEL = {
  BUY: '구매',
  SELL: '판매',
};

/** 내 거래 탭. value가 undefined면 전체. */
export const ROLE_TABS = [
  { value: undefined, label: '전체' },
  { value: 'BUY', label: '구매' },
  { value: 'SELL', label: '판매' },
];
