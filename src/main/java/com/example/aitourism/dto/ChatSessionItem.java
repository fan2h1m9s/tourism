package com.example.aitourism.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatSessionItem {
    private String sessionId;
    private String title;
    private LocalDateTime lastMessageTime;
}
