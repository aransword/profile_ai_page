package dev.aransword.spring_ai_profile.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.WithPayloadSelector;

// Qdrant 헬퍼 메서드들 static import
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantVectorStore {

    // chunkSize: 800토큰, overlap: 100토큰 정도로 설정하여 문맥 연결성을 확보
    private final TokenTextSplitter tokenTextSplitter
            = TokenTextSplitter.builder()
                .withChunkSize(800)           // 청크 하나당 목표 토큰 수
                .withMinChunkSizeChars(100)   // 너무 짧은 조각이 생기지 않도록 방어
                .withKeepSeparator(true)      // 마크다운의 줄바꿈 등 기호를 최대한 보존
                .build();
    private final EmbeddingModel embeddingModel;
    private final QdrantClient qdrantClient;

    // 데이터를 저장할 Qdrant 내 컬렉션 이름 (미리 Qdrant에 생성되어 있어야 함)
    private static final String COLLECTION_NAME = "profile-rag";

    /**
     * 마크다운 텍스트를 청킹하고 Qdrant에 저장하는 메인 메서드입니다.
     */
    public Mono<Void> processAndSave(String documentId, String markdownText, Map<String, Object> metadata) {
        return Mono.fromCallable(() -> {
            log.info("[QdrantVectorStore] 문서 처리 시작. ID: {}", documentId);

            // 1. Spring AI의 Document 객체로 텍스트와 메타데이터를 묶어줍니다.
            Document document = new Document(markdownText, metadata);

            // 2. 스플리터의 apply 메서드에 Document 리스트를 넘겨 청킹을 수행합니다.
            // 반환값 역시 청킹된 Document 객체들의 리스트입니다.
            List<Document> chunkedDocuments = tokenTextSplitter.apply(List.of(document));

            log.info("[QdrantVectorStore] 청킹 완료. 조각 개수: {}", chunkedDocuments.size());

            // Qdrant에 한 번에 삽입할 포인트 리스트 준비
            List<PointStruct> pointsToUpsert = new ArrayList<>();

            // 3. 청킹된 Document 확인
            for (int i = 0; i < chunkedDocuments.size(); i++) {
                Document chunkDoc = chunkedDocuments.get(i);

                // [Step 5] 임베딩 생성
                // Document 객체를 통째로 넘기면 텍스트를 벡터(float 배열)로 변환해 줍니다.
                float[] embeddingVector = embeddingModel.embed(chunkDoc);

                log.info("Chunk {} 임베딩 완료. 벡터 차원 수: {}", i, embeddingVector.length);

                // 1. Vector 변환 (float[] -> List<Float>)
                List<Float> vectorList = new ArrayList<>(embeddingVector.length);
                for (float f : embeddingVector) vectorList.add(f);

                // 2. Payload(메타데이터 + 실제 텍스트) 생성
                Map<String, Value> payload = new HashMap<>();
                payload.put("content", value(chunkDoc.getText())); // RAG 검색 후 가져올 핵심 텍스트
                payload.put("parentDocumentId", value(documentId));
                payload.put("chunkIndex", value(i));

                // 기존 메타데이터들을 Qdrant Value 타입으로 매핑
                chunkDoc.getMetadata().forEach((key, val) -> {
                    if (val instanceof String s) payload.put(key, value(s));
                    else if (val instanceof Number n) payload.put(key, value(n.doubleValue()));
                    else if (val instanceof Boolean b) payload.put(key, value(b));
                    else if (val != null) payload.put(key, value(val.toString()));
                });

                // 3. PointStruct 조립 (Qdrant의 데이터 1건 단위)
                PointStruct point = PointStruct.newBuilder()
                        .setId(id(UUID.randomUUID())) // 각 청크(포인트)의 고유 식별자
                        .setVectors(vectors(vectorList))
                        .putAllPayload(payload)
                        .build();

                pointsToUpsert.add(point);

            }

            // [Step 6] Qdrant에 일괄 저장(Upsert)
            // 비동기(upsertAsync)로 호출하고 .get()으로 기다리지만,
            // 밖에서 boundedElastic 스레드 풀을 쓰기 때문에 WebFlux 메인 스레드는 블로킹되지 않음
            qdrantClient.upsertAsync(COLLECTION_NAME, pointsToUpsert).get();
            log.info("[QdrantVectorStore] {}개의 청크가 '{}' 컬렉션에 성공적으로 저장되었습니다.", pointsToUpsert.size(), COLLECTION_NAME);

            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * 마크다운 텍스트를 쪼개는 내부 로직 (단락 기준)
     */
    private List<String> chunkMarkdown(String text) {
        // 정규식을 사용해 빈 줄(엔터 2번 이상)을 기준으로 텍스트를 분할합니다.
        // 향후 더 정교한 청킹이 필요하다면 Spring AI의 TokenTextSplitter 등을 도입하면 됩니다.
        return Arrays.stream(text.split("\n\\s*\n"))
                .map(String::trim)
                .filter(chunk -> !chunk.isEmpty())
                .toList();
    }

    public Mono<List<String>> searchSimilar(String queryText) {
        return Mono.fromCallable(() -> {
            float[] queryVector = embeddingModel.embed(queryText);
            List<Float> vectorList = new ArrayList<>(queryVector.length);
            for (float f : queryVector) vectorList.add(f);

            // 페이로드 활성화 셀렉터 생성
            WithPayloadSelector withPayload = WithPayloadSelector.newBuilder()
                    .setEnable(true)
                    .build();

            SearchPoints searchRequest = SearchPoints.newBuilder()
                    .setCollectionName(COLLECTION_NAME)
                    .addAllVector(vectorList)
                    .setLimit(3)
                    .setWithPayload(withPayload) // boolean 대신 객체 전달
                    .build();

            List<ScoredPoint> results = qdrantClient.searchAsync(searchRequest).get();

            return results.stream()
                    .map(point -> point.getPayloadMap().get("content").getStringValue())
                    .toList();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}