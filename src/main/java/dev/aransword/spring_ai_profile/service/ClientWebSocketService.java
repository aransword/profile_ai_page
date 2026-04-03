package dev.aransword.spring_ai_profile.service;

import dev.aransword.spring_ai_profile.dto.QueryResponseDto;
import dev.aransword.spring_ai_profile.repository.QdrantVectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClientWebSocketService {

    private static final Duration TYPING_DELAY = Duration.ofMillis(30);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final QdrantVectorStore qdrantVectorStore;

    public ClientWebSocketService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, QdrantVectorStore qdrantVectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemory = chatMemory;
        this.qdrantVectorStore = qdrantVectorStore;
    }

    public Flux<QueryResponseDto> getChatResponseStream(String userMessage, String chatId) {

        // 1. 유사도 검색 수행
        return qdrantVectorStore.searchSimilar(userMessage)
                .flatMapMany(contextList -> {
                    String context = contextList.stream()
                            .collect(Collectors.joining("\n\n"));

                    log.info("검색된 문맥: {}", context);

                    // 2. AI 프롬프트 생성 및 스트리밍
                    return chatClient.prompt()
                            .system(s -> s.text("""
                                    당신은 제공된 [문맥] 정보를 바탕으로 질문에 답변하는 어시스턴트입니다.
                                    제공된 정보에 답변이 없다면 "죄송합니다. 관련 정보를 찾을 수 없습니다."라고 답하세요.
                                    
                                    [문맥]:
                                    {context}
                                    """).param("context", context))
                            .user(userMessage)
                            .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                                    .conversationId(chatId)
                                    .build())
                            .stream()
                            .content()
                            .delayElements(TYPING_DELAY) // 타자기 효과
                            .map(content -> new QueryResponseDto(content, false)); // 일반 메시지 DTO 변환
                })
                // 3. 마지막에 완료 플래그 전송
                .concatWith(Mono.just(new QueryResponseDto("", true)));
    }
}