import { Client } from '@stomp/stompjs';
import { useCallback, useEffect, useRef, useState } from 'react';

/** REST 주소(http://localhost:8080/api)에서 WebSocket 주소(ws://localhost:8080/ws-chat)를 만든다. */
function socketUrl() {
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api';
  const url = new URL(apiBase, window.location.origin);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.pathname = '/ws-chat';
  return url.toString();
}

/**
 * 채팅방 하나에 STOMP로 붙는다.
 * - CONNECT: Authorization 헤더로 JWT 전달 (서버 StompAuthInterceptor가 검증)
 * - SUBSCRIBE /sub/chat/{id}: 이 방 메시지 받기 (참여자만 허용됨)
 *   같은 주소로 거래 알림({ type: 'TRANSACTION', transaction })도 온다 → onTransaction으로 넘긴다
 * - SUBSCRIBE /user/queue/errors: 내 에러만 받기 (Principal 기준 배달)
 * - SEND /pub/chat/{id}: 메시지 보내기 (컨트롤러가 참여자 검사·저장 후 /sub/chat/{id}로 뿌림)
 */
export function useChatSocket(chatRoomId, { onMessage, onTransaction, onError }) {
  const clientRef = useRef(null);
  const handlersRef = useRef({ onMessage, onTransaction, onError });
  const [connected, setConnected] = useState(false);

  // 콜백이 바뀌어도 다시 연결하지 않도록 최신 콜백만 ref에 둔다
  useEffect(() => {
    handlersRef.current = { onMessage, onTransaction, onError };
  }, [onMessage, onTransaction, onError]);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!chatRoomId || !token) {
      return undefined;
    }

    const client = new Client({
      brokerURL: socketUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/sub/chat/${chatRoomId}`, (frame) => {
          const body = JSON.parse(frame.body);
          if (body.type === 'TRANSACTION') {
            handlersRef.current.onTransaction?.(body.transaction);
          } else {
            handlersRef.current.onMessage?.(body);
          }
        });
        client.subscribe('/user/queue/errors', (frame) => {
          handlersRef.current.onError?.(frame.body);
        });
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => {
        handlersRef.current.onError?.(frame.headers.message || '채팅 서버 오류가 발생했습니다.');
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [chatRoomId]);

  const send = useCallback(
    (content) => {
      const client = clientRef.current;
      if (!client?.connected) {
        return false;
      }
      client.publish({ destination: `/pub/chat/${chatRoomId}`, body: JSON.stringify({ content }) });
      return true;
    },
    [chatRoomId],
  );

  return { connected, send };
}
