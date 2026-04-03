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
    private static final int SEARCH_RESULT_LIMIT = 3;

    /**
     * 마크다운 텍스트를 청킹하고 Qdrant에 저장하는 메인 메서드입니다.
     */
    public Mono<Void> processAndSave(String documentId, String markdownText, Map<String, Object> metadata) {
        return Mono.fromCallable(() -> {
            log.info("[QdrantVectorStore] 문서 처리 시작. ID: {}", documentId);

            // 1. 텍스트를 청킹합니다.
            List<Document> chunkedDocuments = chunkDocument(markdownText, metadata);
            log.info("[QdrantVectorStore] 청킹 완료. 조각 개수: {}", chunkedDocuments.size());

            // 2. 각 청크를 PointStruct로 변환합니다.
            List<PointStruct> pointsToUpsert = new ArrayList<>();
            for (int i = 0; i < chunkedDocuments.size(); i++) {
                pointsToUpsert.add(buildPoint(chunkedDocuments.get(i), documentId, i));
            }

            // 3. Qdrant에 일괄 저장(Upsert)
            qdrantClient.upsertAsync(COLLECTION_NAME, pointsToUpsert).get();
            log.info("[QdrantVectorStore] {}개의 청크가 '{}' 컬렉션에 성공적으로 저장되었습니다.", pointsToUpsert.size(), COLLECTION_NAME);

            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    public Mono<List<String>> searchSimilar(String queryText) {
        return Mono.fromCallable(() -> {
            List<Float> vectorList = toFloatList(embeddingModel.embed(queryText));

            WithPayloadSelector withPayload = WithPayloadSelector.newBuilder()
                    .setEnable(true)
                    .build();

            SearchPoints searchRequest = SearchPoints.newBuilder()
                    .setCollectionName(COLLECTION_NAME)
                    .addAllVector(vectorList)
                    .setLimit(SEARCH_RESULT_LIMIT)
                    .setWithPayload(withPayload)
                    .build();

            List<ScoredPoint> results = qdrantClient.searchAsync(searchRequest).get();

            return results.stream()
                    .map(point -> point.getPayloadMap().get("content").getStringValue())
                    .toList();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ──────────────────────────────────────────────
    // Private Helper Methods
    // ──────────────────────────────────────────────

    /**
     * 텍스트와 메타데이터를 Document로 감싼 뒤 청킹합니다.
     */
    private List<Document> chunkDocument(String text, Map<String, Object> metadata) {
        Document document = new Document(text, metadata);
        return tokenTextSplitter.apply(List.of(document));
    }

    /**
     * float 배열을 List<Float>로 변환합니다.
     */
    private List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float f : vector) list.add(f);
        return list;
    }

    /**
     * 청크 Document의 메타데이터를 Qdrant Payload(Map<String, Value>)로 변환합니다.
     */
    private Map<String, Value> buildPayload(Document chunkDoc, String documentId, int chunkIndex) {
        Map<String, Value> payload = new HashMap<>();
        payload.put("content", value(chunkDoc.getText()));
        payload.put("parentDocumentId", value(documentId));
        payload.put("chunkIndex", value(chunkIndex));

        chunkDoc.getMetadata().forEach((key, val) -> {
            if (val instanceof String s) payload.put(key, value(s));
            else if (val instanceof Number n) payload.put(key, value(n.doubleValue()));
            else if (val instanceof Boolean b) payload.put(key, value(b));
            else if (val != null) payload.put(key, value(val.toString()));
        });

        return payload;
    }

    /**
     * 청크 Document 하나를 임베딩하고 Qdrant PointStruct로 조립합니다.
     */
    private PointStruct buildPoint(Document chunkDoc, String documentId, int chunkIndex) {
        float[] embeddingVector = embeddingModel.embed(chunkDoc);
        log.info("Chunk {} 임베딩 완료. 벡터 차원 수: {}", chunkIndex, embeddingVector.length);

        return PointStruct.newBuilder()
                .setId(id(UUID.randomUUID()))
                .setVectors(vectors(toFloatList(embeddingVector)))
                .putAllPayload(buildPayload(chunkDoc, documentId, chunkIndex))
                .build();
    }
}