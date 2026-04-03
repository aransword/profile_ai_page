package dev.aransword.spring_ai_profile.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aransword.spring_ai_profile.dto.QueryRequestDto;
import dev.aransword.spring_ai_profile.dto.QueryResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientWebSocketHandler implements WebSocketHandler {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper; // JSON 변환용
    private final ChatMemory chatMemory;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String chatId = session.getId();

        ChatClient chatClient = chatClientBuilder.build();

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> {
                    try {
                        // 1. 클라이언트가 보낸 JSON을 DTO로 변환
                        QueryRequestDto request = objectMapper.readValue(payload, QueryRequestDto.class);

                        // 2. Spring AI를 통해 Gemini의 응답을 스트림(Flux)으로 받음
                        Flux<String> aiResponseStream = chatClient.prompt()
                                .user(request.message())
                                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                                        .conversationId(chatId) // 대화 식별 ID
                                        .build())
                                .stream()
                                .content()
                                .delayElements(java.time.Duration.ofMillis(30));

                        // 3. AI 응답 조각들을 QueryResponseDTO로 변환하여 전송
                        return session.send(
                                aiResponseStream
                                        .map(content -> toJson(new QueryResponseDto(content, false)))
                                        .concatWith(Mono.just(toJson(new QueryResponseDto("", true)))) // 마지막 종료 신호
                                        .map(session::textMessage)
                        );
                    } catch (Exception e) {
                        log.error("메시지 처리 중 에러 발생", e);
                        return Mono.empty();
                    }
                })
                .then();
    }

    // 객체를 JSON 문자열로 변환하는 헬퍼 메서드
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}