package dev.aransword.spring_ai_profile.service;

import dev.aransword.spring_ai_profile.repository.QdrantVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final QdrantVectorStore qdrantVectorStore;

    /**
     * FilePart를 받아 마크다운 텍스트로 변환하고 Qdrant 벡터 스토어 저장을 위한 준비를 합니다.
     */
    public Mono<String> processMarkdownFile(FilePart filePart) {

        // 1. UUID 및 파일명 추출
        String originalFilename = filePart.filename();
        String documentId = UUID.randomUUID().toString();

        log.info("마크다운 문서 처리 시작. 파일: {}, ID: {}", originalFilename, documentId);

        // 2. 메타데이터 구성 (검색 시 출처 등으로 활용)
        Map<String, Object> docMetadata = new HashMap<>();
        docMetadata.put("originalFilename", originalFilename != null ? originalFilename : "");
        docMetadata.put("uploadTime", System.currentTimeMillis());

        // 3. 파일 스트림 처리 및 텍스트 변환
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);

                    // 메모리 해제
                    DataBufferUtils.release(dataBuffer);

                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .flatMap(markdownText -> {
                    // QdrantVectorStore에 텍스트와 메타데이터를 넘겨 청킹 및 저장을 위임합니다.
                    return qdrantVectorStore.processAndSave(documentId, markdownText, docMetadata)
                            // 저장이 무사히 끝나면(.then), 컨트롤러에게 documentId를 반환합니다.
                            .then(Mono.just(documentId));
                })
                .doOnSuccess(id -> log.info("마크다운 문서 처리 및 파이프라인 전송 완료. ID: {}", id))
                // 예외 처리 (참고 코드의 catch 블록 역할)
                .onErrorMap(e -> {
                    log.error("문서 처리 중 오류 발생: {}", e.getMessage(), e);
                    // WebFlux 환경에 맞게 예외를 매핑해서 던집니다.
                    // (필요하다면 DocumentProcessingException 같은 커스텀 예외 클래스를 만들어 사용하세요)
                    return new RuntimeException("문서 처리 중 오류: " + e.getMessage(), e);
                });
    }
}