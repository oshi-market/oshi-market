import { Link } from 'react-router-dom';
import mockItems from './mockItems';

/** 백엔드 ItemStatus enum 기준. feature/item 머지 후에는 item/constants.js의 STATUS_LABEL로 교체. */
const STATUS_LABEL = {
  SELLING: '판매중',
  IN_TRANSACTION: '거래중',
  SOLD_OUT: '판매완료',
};

function HomeItemGrid() {
  return (
    <section className="home-items">
      <h2 className="home-items-heading">방금 올라온 굿즈</h2>
      <div className="home-item-grid">
        {mockItems.map((item) => (
          <Link to={`/items/${item.id}`} key={item.id} className="home-item-card">
            <div className="home-item-thumb">
              <img src={item.thumbnailUrl} alt={item.title} loading="lazy" />
              <span className={`home-item-badge home-item-badge-${item.status.toLowerCase()}`}>
                {STATUS_LABEL[item.status]}
              </span>
            </div>
            <div className="home-item-body">
              <p className="home-item-title">{item.title}</p>
              <p className="home-item-price">{item.price.toLocaleString()}원</p>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

export default HomeItemGrid;
