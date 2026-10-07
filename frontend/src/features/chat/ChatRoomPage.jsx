import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { fetchItem } from '../item/api';
import { useAuth } from '../member/useAuth';
import { fetchMyTransactions } from '../transaction/api';
import { fetchMessages, fetchMyChatRooms } from './api';
import TradePanel from './TradePanel';
import { useChatSocket } from './useChatSocket';
import './chat.css';

function formatTime(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ''
    : date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
}

/**
 * 이 채팅방의 가장 최근 거래. 방 단위 거래 조회 API가 아직 없어 내 거래 목록에서 찾는다.
 * (개선 후보: GET /api/chatrooms/{id}/transaction)
 */
async function findLatestTransaction(chatRoomId) {
  const { data } = await fetchMyTransactions({ size: 100 });
  const list = (data.content ?? []).filter((t) => t.chatRoomId === chatRoomId);
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  return list[0] ?? null;
}

function ChatRoomPage() {
  const chatRoomId = Number(useParams().chatRoomId);
  const { member } = useAuth();
  const [room, setRoom] = useState(null);
  const [item, setItem] = useState(null);
  const [messages, setMessages] = useState([]);
  const [transaction, setTransaction] = useState(null);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');
  const [socketError, setSocketError] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const bottomRef = useRef(null);

  const refreshTransaction = useCallback(() => {
    findLatestTransaction(chatRoomId).then(setTransaction).catch(() => {});
  }, [chatRoomId]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setIsLoading(true);
      setError('');
      try {
        const { data: rooms } = await fetchMyChatRooms();
        const found = rooms.find((r) => r.id === chatRoomId);
        if (!found) {
          throw new Error('참여하지 않은 채팅방입니다.');
        }
        const [{ data: itemData }, { data: page }, latest] = await Promise.all([
          fetchItem(found.itemId),
          fetchMessages(chatRoomId, { size: 50 }),
          findLatestTransaction(chatRoomId),
        ]);
        if (cancelled) return;
        setRoom(found);
        setItem(itemData);
        setMessages([...(page.content ?? [])].reverse()); // 서버는 최신순 → 화면은 오래된 것부터
        setTransaction(latest);
      } catch (err) {
        if (!cancelled) setError(err.response ? getErrorMessage(err) : err.message);
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [chatRoomId]);

  const handleMessage = useCallback((message) => {
    setMessages((prev) => (prev.some((m) => m.id === message.id) ? prev : [...prev, message]));
  }, []);

  const handleSocketError = useCallback((message) => setSocketError(message), []);

  const { connected, send } = useChatSocket(room ? chatRoomId : null, {
    onMessage: handleMessage,
    onError: handleSocketError,
  });

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' });
  }, [messages]);

  function handleSubmit(event) {
    event.preventDefault();
    const content = draft.trim();
    if (!content) return;
    setSocketError('');
    if (send(content)) {
      setDraft('');
    } else {
      setSocketError('채팅 서버에 연결되지 않았습니다. 잠시 후 다시 시도해 주세요.');
    }
  }

  if (isLoading) {
    return (
      <div className="page">
        <p className="muted">불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page">
        <p className="form-error">{error}</p>
        <Link to="/chat" className="btn btn-ghost btn-sm">
          채팅 목록으로
        </Link>
      </div>
    );
  }

  const isBuyer = member?.id === room.buyerId;
  const isSeller = member?.id === room.sellerId;

  return (
    <div className="page chat-room-page">
      <div className="chat-room-header">
        <h2>
          <Link to={`/items/${room.itemId}`}>{item?.title ?? `상품 #${room.itemId}`}</Link>
        </h2>
        <span className={`chat-connection${connected ? ' is-online' : ''}`}>
          {connected ? '● 연결됨' : '○ 연결 중...'}
        </span>
      </div>

      <TradePanel
        chatRoomId={chatRoomId}
        isBuyer={isBuyer}
        isSeller={isSeller}
        transaction={transaction}
        onChange={setTransaction}
        onRefresh={refreshTransaction}
      />

      <div className="chat-messages">
        {messages.length === 0 && <p className="muted">첫 메시지를 보내 보세요.</p>}
        {messages.map((m) => (
          <div key={m.id} className={`chat-bubble${m.senderId === member?.id ? ' is-mine' : ''}`}>
            {m.content}
            <span className="chat-bubble-time">{formatTime(m.createdAt)}</span>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      {socketError && (
        <p className="form-error" role="alert">
          {socketError}
        </p>
      )}

      <form className="chat-input" onSubmit={handleSubmit}>
        <input
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="메시지를 입력하세요"
          maxLength={1000}
        />
        <button type="submit" className="btn btn-primary" disabled={!connected || !draft.trim()}>
          보내기
        </button>
      </form>
    </div>
  );
}

export default ChatRoomPage;
