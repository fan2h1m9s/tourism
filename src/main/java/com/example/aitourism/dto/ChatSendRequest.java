package com.example.aitourism.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSendRequest {

    private String sessionId;

    @NotBlank(message = "userId 不能为空")
    private String userId;

    @NotBlank(message = "message 不能为空")
    private String message;
}
