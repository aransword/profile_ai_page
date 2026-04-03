import ChatArea from '../components/ChatArea';
import MessageInput from '../components/MessageInput';
import { useChat } from '../hooks/useChat';

export default function ChatPage() {
  const { messages, isGenerating, sendMessage } = useChat();

  return (
    <div className="flex h-screen w-full bg-gemini-bg font-sans">
      <div className="flex-1 flex flex-col relative max-w-5xl mx-auto w-full">
        <header className="h-16 flex items-center px-4 sm:px-8 text-xl font-semibold text-gray-800">
          Spring AI Chat
        </header>
        
        <ChatArea messages={messages} />
        
        <div className="bg-gemini-bg pt-2 pb-6">
          <MessageInput onSendMessage={sendMessage} isGenerating={isGenerating} />
        </div>
      </div>
    </div>
  );
}