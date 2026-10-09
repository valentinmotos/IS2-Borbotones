package com.youtics.app_spring_ai.dto;

public record ChatFullResponse(String content, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
}
