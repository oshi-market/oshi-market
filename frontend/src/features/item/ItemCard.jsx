import { Link } from 'react-router-dom';
import ItemThumb from './ItemThumb';
import { CONDITION_LABEL, STATUS_LABEL } from './constants';

/** 상품 목록 카드 (사진·제목·가격·상태). item-grid 안에서 쓴다. */
function ItemCard({ item }) {
  return (
    <Link to={`/items/${item.id}`} className="item-card">
      <ItemThumb url={item.thumbnailUrl} alt={item.title} />
      <div className="item-card-body">
        <p className="item-card-title">{item.title}</p>
        <p className="item-card-price">{item.price.toLocaleString()}원</p>
        <div className="item-card-meta">
          <span className={`badge badge-status-${item.status.toLowerCase()}`}>
            {STATUS_LABEL[item.status]}
          </span>
          <span className="badge badge-outline">{CONDITION_LABEL[item.condition]}</span>
        </div>
      </div>
    </Link>
  );
}

export default ItemCard;
