package com.example.aitourism.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatHistoryItem {
    private String msgId;
    private String role;
    private String content;
    private LocalDateTime createdTime;
}
