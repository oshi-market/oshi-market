import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { fetchItem } from '../item/api';
import { useAuth } from '../member/useAuth';
import { fetchMyChatRooms } from './api';
import './chat.css';

/** 내가 참여한 채팅방 목록. 방 응답에는 상품명이 없어 상품을 한 번씩 조회해 붙인다. */
function ChatListPage() {
  const { member } = useAuth();
  const [rooms, setRooms] = useState([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const { data } = await fetchMyChatRooms();
        const withItems = await Promise.all(
          data.map(async (room) => {
            try {
              const { data: item } = await fetchItem(room.itemId);
              return { ...room, itemTitle: item.title };
            } catch {
              return { ...room, itemTitle: `상품 #${room.itemId}` };
            }
          }),
        );
        if (!cancelled) setRooms(withItems);
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err, '채팅방 목록을 불러오지 못했습니다.'));
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="page">
      <h2>채팅</h2>
      {isLoading && <p className="muted">불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && rooms.length === 0 && (
        <p className="muted">아직 채팅방이 없어요. 상품 상세에서 '채팅하기'를 눌러 보세요.</p>
      )}
      <ul className="chat-list">
        {rooms.map((room) => (
          <li key={room.id}>
            <Link to={`/chat/${room.id}`} className="chat-list-item">
              <span>{room.itemTitle}</span>
              <span className="badge badge-outline">{room.buyerId === member?.id ? '구매' : '판매'}</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default ChatListPage;
