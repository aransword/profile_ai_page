package dev.aransword.spring_ai_profile.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aransword.spring_ai_profile.dto.QueryRequestDto;
import dev.aransword.spring_ai_profile.dto.QueryResponseDto;
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
    private final ClientWebSocketService chatService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("웹 소켓 연결 요청: {}", session.getId());

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> {
                    try {
                        QueryRequestDto request = objectMapper.readValue(payload, QueryRequestDto.class);

                        return session.send(
                                chatService.getChatResponseStream(request.message(), session.getId())
                                        .map(this::toJson)
                                        .map(session::textMessage)
                        );
                    } catch (Exception e) {
                        log.error("메시지 처리 중 에러", e);
                        // 클라이언트에게 에러 상황을 알려주는 응답 전송
                        QueryResponseDto errorResponse = new QueryResponseDto("오류가 발생했습니다: " + e.getMessage(), true);
                        return session.send(Mono.just(session.textMessage(toJson(errorResponse))));
                    }
                })
                .then();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON 직렬화 실패", e);
            return "{\"content\":\"응답 변환 중 오류가 발생했습니다.\",\"isComplete\":true}";
        }
    }
}