package dev.aransword.spring_ai_profile.config;

import dev.aransword.spring_ai_profile.handler.ClientWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final ClientWebSocketHandler clientWebSocketHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        // "/ws/chat" 경로로 들어오는 웹소켓 연결을 핸들러에 매핑합니다.
        Map<String, Object> map = Map.of("/ws/chat", clientWebSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(1);
        mapping.setUrlMap(map);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}