package com.treserve.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Профиль пользователя")
public class UserProfileResponse {

    @Schema(description = "ID пользователя", example = "1")
    private Long id;

    @Schema(description = "Email пользователя", example = "user@example.com")
    private String email;

    @Schema(description = "Имя пользователя", example = "Иван")
    private String name;

    @Schema(description = "Роль пользователя", example = "USER", allowableValues = {"USER", "ADMIN"})
    private String role;
}