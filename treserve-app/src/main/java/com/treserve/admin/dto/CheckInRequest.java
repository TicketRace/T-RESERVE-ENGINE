package com.treserve.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Запрос на проверку билета (check-in)")
public class CheckInRequest {

    @NotNull
    @Schema(description = "UUID токен из QR-кода", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID token;
}