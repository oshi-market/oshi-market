import { imageUrl } from './imageUrl';

/** 상품 카드 썸네일. 사진이 없으면 기존처럼 🖼️ 자리표시자. */
function ItemThumb({ url, alt, className = 'item-card-thumb' }) {
  return (
    <div className={className}>
      {url ? <img src={imageUrl(url, 'thumb')} alt={alt} loading="lazy" /> : <span aria-hidden="true">🖼️</span>}
    </div>
  );
}

export default ItemThumb;
