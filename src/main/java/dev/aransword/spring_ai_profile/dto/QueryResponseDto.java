package dev.aransword.spring_ai_profile.dto;

public record QueryResponseDto(
        String content,      // 쪼개져서 날아오는 AI의 답변 텍스트 조각
        boolean isComplete   // 이 조각이 마지막 응답인지 여부
) {
    // 편의를 위한 정적 팩토리 메서드
    public static QueryResponseDto chunk(String content) {
        return new QueryResponseDto(content, false);
    }

    public static QueryResponseDto done() {
        return new QueryResponseDto("", true);
    }
}