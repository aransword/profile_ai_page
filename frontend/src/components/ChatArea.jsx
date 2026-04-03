import { useEffect, useRef } from 'react';
import UserMessage from './message/UserMessage'
import AiMessage from './message/AiMessage';

export default function ChatArea({ messages }) {
  const scrollRef = useRef(null);

  // 메시지가 추가될 때마다 맨 아래로 자동 스크롤
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  return (
    <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 sm:p-8 scroll-smooth">
      <div className="max-w-3xl mx-auto flex flex-col gap-8">
        
        {messages.length === 0 ? (
          <div className="text-center text-gray-400 mt-20 text-2xl font-semibold">
            무엇을 도와드릴까요?
          </div>
        ) : (
          messages.map((msg, index) => (
            // 💡 롤(role)에 따라 분리된 컴포넌트를 호출합니다!
            msg.role === 'user' ? (
              <UserMessage key={index} content={msg.content} />
            ) : (
              <AiMessage 
                key={index} 
                content={msg.content} 
                isGenerating={msg.isGenerating} 
              />
            )
          ))
        )}
        
      </div>
    </div>
  );
}