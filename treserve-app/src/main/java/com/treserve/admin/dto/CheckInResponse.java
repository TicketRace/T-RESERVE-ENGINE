package com.treserve.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Ответ на проверку билета")
public class CheckInResponse {

    @Schema(description = "Сообщение о результате", example = "Checked in successfully")
    private String message;

    @Schema(description = "Статус билета после проверки", example = "USED")
    private String status;
}