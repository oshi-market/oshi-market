/**
 * Cloudinary URL 변환. 원본 URL의 /upload/ 뒤에 변환 옵션을 끼우면 Cloudinary가 리사이즈·압축한
 * 이미지를 CDN으로 내려준다 (원본은 그대로). Cloudinary URL이 아니면 원본을 그대로 쓴다.
 */
const TRANSFORMS = {
  thumb: 'c_fill,w_400,h_400,f_auto,q_auto',
  detail: 'c_limit,w_1200,h_1200,f_auto,q_auto',
  mini: 'c_fill,w_120,h_120,f_auto,q_auto',
};

export function imageUrl(url, size = 'thumb') {
  if (!url || !url.includes('/upload/')) {
    return url;
  }
  return url.replace('/upload/', `/upload/${TRANSFORMS[size]}/`);
}

/** 백엔드 ItemImageService 검증과 같은 값. 서버에서도 한 번 더 검사한다. */
export const MAX_IMAGES = 5;
export const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
