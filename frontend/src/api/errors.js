/** 백엔드 공통 에러 응답 포맷({ code, message, timestamp })에서 메시지만 뽑아낸다. */
export function getErrorMessage(error, fallback = '알 수 없는 오류가 발생했습니다.') {
  return error?.response?.data?.message ?? fallback;
}
