import { useState, useEffect, useRef } from 'react';

export function useChat(wsUrl = 'ws://localhost:9090/ws/chat') {
  const [messages, setMessages] = useState([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const ws = useRef(null);

  const bufferRef = useRef("");
  const displayIndexRef = useRef(0);
  const isWsDoneRef = useRef(false);

  // 1. 웹소켓 연결 및 수신 로직
  useEffect(() => {
    ws.current = new WebSocket(wsUrl);

    ws.current.onopen = () => console.log('WebSocket 연결 성공!');
    ws.current.onclose = () => console.log('WebSocket 연결 종료');
    ws.current.onerror = (error) => console.error('WebSocket 에러:', error);

    ws.current.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.isComplete) {
        isWsDoneRef.current = true;
      } else {
        bufferRef.current += data.content;
      }
    };

    return () => {
      if (ws.current) ws.current.close();
    };
  }, [wsUrl]);

  // 2. 타이핑(스트리밍) 애니메이션 로직
  useEffect(() => {
    const typingTimer = setInterval(() => {
      if (isGenerating && displayIndexRef.current < bufferRef.current.length) {
        displayIndexRef.current += 1;
        const currentText = bufferRef.current.substring(0, displayIndexRef.current);

        setMessages((prev) => {
          const updated = [...prev];
          const lastIndex = updated.length - 1;
          if (lastIndex >= 0) {
            updated[lastIndex] = { ...updated[lastIndex], content: currentText };
          }
          return updated;
        });
      }

      if (
        isWsDoneRef.current &&
        displayIndexRef.current === bufferRef.current.length &&
        bufferRef.current.length > 0
      ) {
        setIsGenerating(false);
        isWsDoneRef.current = false;

        setMessages((prev) => {
          const updated = [...prev];
          const lastIndex = updated.length - 1;
          if (lastIndex >= 0) {
            updated[lastIndex] = { ...updated[lastIndex], isGenerating: false };
          }
          return updated;
        });
      }
    }, 20);

    return () => clearInterval(typingTimer);
  }, [isGenerating]);

  // 3. 메시지 전송 로직
  const sendMessage = (text) => {
    if (!ws.current || ws.current.readyState !== WebSocket.OPEN) {
      alert("서버와 연결되어 있지 않습니다.");
      return;
    }

    bufferRef.current = "";
    displayIndexRef.current = 0;
    isWsDoneRef.current = false;

    const userMessage = { role: 'user', content: text };
    const aiPlaceholder = { role: 'ai', content: '', isGenerating: true };

    setMessages((prev) => [...prev, userMessage, aiPlaceholder]);
    setIsGenerating(true);

    const payload = JSON.stringify({ message: text });
    ws.current.send(payload);
  };

  // 💡 컴포넌트에서 가져다 쓸 데이터와 함수만 쏙 빼서 반환합니다!
  return {
    messages,
    isGenerating,
    sendMessage,
  };
}