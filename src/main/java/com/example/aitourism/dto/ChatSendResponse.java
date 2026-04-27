package com.example.aitourism.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatSendResponse {
    private String sessionId;
    private String answer;
}
