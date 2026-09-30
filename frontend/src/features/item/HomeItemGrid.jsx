import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchItems } from './api';
import { STATUS_LABEL } from './constants';
import { imageUrl } from './imageUrl';

/** 홈 "방금 올라온 굿즈": 최신 상품 8개. 불러오기 실패하거나 상품이 없으면 섹션을 숨긴다. */
function HomeItemGrid() {
  const [items, setItems] = useState([]);

  useEffect(() => {
    let cancelled = false;
    fetchItems({ size: 8 })
      .then(({ data }) => {
        if (!cancelled) setItems(data.content);
      })
      .catch(() => {
        if (!cancelled) setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (items.length === 0) {
    return null;
  }

  return (
    <section className="home-items">
      <h2 className="home-items-heading">방금 올라온 굿즈</h2>
      <div className="home-item-grid">
        {items.map((item) => (
          <Link to={`/items/${item.id}`} key={item.id} className="home-item-card">
            <div className="home-item-thumb">
              {item.thumbnailUrl ? (
                <img src={imageUrl(item.thumbnailUrl, 'thumb')} alt={item.title} loading="lazy" />
              ) : (
                <span className="home-item-placeholder" aria-hidden="true">
                  🖼️
                </span>
              )}
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
