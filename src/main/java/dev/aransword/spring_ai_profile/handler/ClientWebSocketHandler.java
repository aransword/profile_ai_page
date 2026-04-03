package dev.aransword.spring_ai_profile.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aransword.spring_ai_profile.dto.QueryRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import dev.aransword.spring_ai_profile.service.ClientWebSocketService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ClientWebSocketService chatService; // 💡 서비스 주입

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("웹 소켓 연결 요청: {}", session.getId());

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> {
                    try {
                        QueryRequestDto request = objectMapper.readValue(payload, QueryRequestDto.class);

                        // 💡 서비스 호출하여 비즈니스 로직 수행
                        return session.send(
                                chatService.getChatResponseStream(request.message(), session.getId())
                                        .map(this::toJson)
                                        .map(session::textMessage)
                        );
                    } catch (Exception e) {
                        log.error("메시지 처리 중 에러", e);
                        return Mono.empty();
                    }
                })
                .then();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}