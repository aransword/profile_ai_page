package dev.aransword.spring_ai_profile;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    // ChatClient.Builder를 주입받아 빌드합니다.
    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 간단한 동기식(WebFlux 환경이지만 blocking 방식) 호출 테스트
    @GetMapping("/test/chat")
    public Mono<String> testChat(@RequestParam(defaultValue = "안녕! 넌 누구니?") String message) {
        // ChatClient를 사용하여 Gemini에게 메시지를 보내고 응답을 받습니다.
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();

        // WebFlux 환경이므로 결과를 Mono로 감싸서 반환합니다.
        return Mono.just(response);
    }
}