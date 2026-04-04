# Spring AI Profile (RAG Chatbot Project)

이 프로젝트는 **Spring Boot (WebFlux)**, **Spring AI**, **Qdrant (Vector DB)** 를 활용하여 구축된 RAG(Retrieval-Augmented Generation) 기반 챗봇 웹 애플리케이션입니다.
마크다운(.md) 문서를 업로드해 지식 베이스를 구축하고, 해당 문서 내용을 바탕으로 실시간 스트리밍 방식의 채팅이 가능한 기능을 제공합니다. 프론트엔드는 **React (Vite)** 와 Tailwind CSS를 활용하여 구축되었습니다.

## 🚀 주요 기능 (Features)

1. **마크다운 문서 업로드 및 임베딩 (RAG)**
   - 마크다운 문서를 업로드하면 텍스트를 적절한 크기로 청킹(Chunking)합니다.
   - Google Gemini Embedding 모델을 사용하여 텍스트를 벡터로 변환 후 Qdrant Vector Database에 저장합니다.
2. **실시간 스트리밍 채팅 (WebSocket)**
   - 사용자의 질문에 대해 Qdrant에서 유사도 검색을 수행하여 관련 문맥(Context)을 추출합니다.
   - 검색된 문맥을 바탕으로 Google Gemini Pro 모델이 답변을 생성합니다.
   - WebSocket을 통해 생성된 답변이 타자기 효과처럼 사용자에게 실시간으로 스트리밍되어 표시됩니다.
3. **분리된 UI 환경**
   - **채팅 페이지 (`/`)**: 메인 화면으로, 채팅 내역과 실시간 응답을 확인할 수 있습니다.
   - **문서 업로드 페이지 (`/upload`)**: RAG 지식 베이스(Qdrant)에 참고 문서를 추가할 수 있는 페이지입니다.

---

## 🛠️ 기술 스택 (Tech Stack)

### Backend
- **Java 17**
- **Spring Boot 3.x (WebFlux)**: 비동기 논블로킹 통신 및 WebSocket 지원
- **Spring AI**: Google Gemini AI 연동 (`gemini-3.1-flash-lite-preview`, `gemini-embedding-001`)
- **Qdrant**: Vector Database (유사도 검색)
- **Lombok**, **Jackson** 등

### Frontend
- **React 19**
- **Vite**
- **Tailwind CSS 4**: UI 스타일링
- **React Router DOM**: 페이지 라우팅
- **React Markdown**: 채팅 마크다운 렌더링

---

## ⚙️ 사전 준비사항 (Prerequisites)

프로젝트를 실행하기 위해 다음 환경이 준비되어 있어야 합니다.

- **Java 17** 이상
- **Node.js** 18 이상 (프론트엔드 구동용)
- **Google Cloud API Key** (Gemini API 사용)
- **Qdrant** (로컬 Docker 실행 또는 클라우드 클러스터 엔드포인트/API 키)

---

## 🔐 환경 변수 설정 (Environment Variables)

백엔드를 실행하기 위해 프로젝트 루트, 또는 환경 변수에 다음 값들이 설정되어야 합니다. (아래 항목들을 `.env` 파일에 작성하여 활용 가능)

```env
# Google Gemini API
GOOGLE_CLOUD_PROJECT_ID=your-google-cloud-project-id
GOOGLE_AI_API_KEY=your-google-ai-api-key

# Qdrant Vector DB
QDRANT_CLUSTER_ENDPOINT=your-qdrant-endpoint (또는 localhost)
QDRANT_API_KEY=your-qdrant-api-key (로컬 환경에서는 생략하거나 빈 문자열 가능)
```

## 🚀 실행 가이드 (Getting Started)

### 1. Backend (Spring Boot) 실행

```bash
# 프로젝트 루트 디렉토리에서 실행
./gradlew bootRun
```
- 기본적으로 Spring Boot 애플리케이션은 `http://localhost:9090` 포트에서 실행됩니다.

### 2. Frontend (React) 실행

```bash
# frontend 디렉토리로 이동
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```
- 프론트엔드 개발 서버가 시작되면 브라우저에서 `http://localhost:5173` (기본값)으로 접속할 수 있습니다.

---

## 📁 프로젝트 주요 구조 (Project Structure)

```text
spring-ai-profile/
├── src/main/java/dev/aransword/spring_ai_profile/
│   ├── config/             # Qdrant, WebSocket 등 설정 클래스
│   ├── controller/         # Markdown 파일 업로드 REST API 컨트롤러
│   ├── dto/                # 채팅 요청 및 응답 DTO
│   ├── handler/            # WebSocket 메시지 처리 핸들러
│   ├── repository/         # QdrantVectorStore 연동 및 임베딩 처리
│   └── service/            # RAG 프로세스 및 WebSocket 스트리밍 서비스 로직
├── src/main/resources/
│   └── application.yaml    # Spring Boot 애플리케이션 설정 파일
│
└── frontend/               # React 프론트엔드 애플리케이션 루트
    ├── src/
    │   ├── components/     # UI 컴포넌트 (ChatArea, MessageInput 등)
    │   ├── hooks/          # 커스텀 훅 (웹소켓 통신을 관리하는 useChat 등)
    │   ├── pages/          # ChatPage, UploadPage 페이지 컴포넌트
    │   ├── App.jsx         # 메인 라우터 진입점
    │   └── main.jsx        # React 마운트 진입점
    └── package.json        
```

## 💡 주요 동작 흐름

1. `/upload` 페이지에서 마크다운 문서를 서버로 전송합니다.
2. `RagService.java`에서 텍스트를 읽고 `QdrantVectorStore.java`를 통해 문서를 토큰 단위(TokenTextSplitter)로 나눕니다.
3. 분할된 각 청크(Chunk)는 `Spring AI EmbeddingModel`을 거쳐 벡터값으로 변환된 뒤, `Qdrant` 컬렉션(`profile-rag`)에 메타데이터(파일명, 저장 시간 등)와 함께 저장됩니다.
4. `/` (채팅 페이지)에서 사용자가 질문을 입력하면, WebSocket(`useChat.js`)을 통해 백엔드로 전달됩니다.
5. `QdrantVectorStore.java`에서 질문을 임베딩하고 유사한 문맥(Context)을 3개까지 가져옵니다.
6. `ClientWebSocketService.java`에서 System 프롬프트에 문맥 정보를 주입한 후 Google GenAI 모델을 호출합니다.
7. 생성되는 답변 조각(Stream)이 실시간으로 WebSocket을 통해 클라이언트에게 반환되며 타자기 효과와 함께 화면에 나타납니다.
