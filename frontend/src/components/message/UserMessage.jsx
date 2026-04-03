export default function UserMessage({ content }) {
  return (
    <div className="flex justify-end">
      <div className="max-w-[85%] rounded-2xl px-5 py-3 text-base leading-relaxed bg-gemini-user text-black rounded-br-none whitespace-pre-wrap break-words">
        {content}
      </div>
    </div>
  );
}