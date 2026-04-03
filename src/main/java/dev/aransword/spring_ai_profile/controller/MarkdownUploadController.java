package dev.aransword.spring_ai_profile.controller;

import dev.aransword.spring_ai_profile.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/rag")
public class MarkdownUploadController {

    private final RagService ragService;

    // 생성자 주입 (Spring 4.3 이후 @Autowired 생략 가능)
    public MarkdownUploadController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadMarkdown(@RequestPart("file") FilePart filePart) {

        log.info(filePart.filename() + " 업로드 요청");

        // 1. 컨트롤러 역할: 파라미터 및 기초 검증
        if (filePart.filename() == null || !filePart.filename().toLowerCase().endsWith(".md")) {
            log.error("적절하지 않은 파일 업로드");
            return Mono.error(new IllegalArgumentException("마크다운(.md) 파일만 업로드 가능합니다."));
        }

        // 2. 컨트롤러 역할: 서비스로 비즈니스 로직 위임 및 응답 조립
        return ragService.processMarkdownFile(filePart)
                .map(serviceResult -> "업로드 성공: " + filePart.filename() + " - " + serviceResult)
                // 에러 처리
                .onErrorResume(e -> Mono.just("업로드 실패: " + e.getMessage()));
    }
}