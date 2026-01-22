package com.javation.coloringbook.DTO.response;

import com.javation.coloringbook.Entity.Users;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
public class UserResponseDTO {

    public final Long id;
    public final String email;
    public final LocalDateTime createAt;

    public UserResponseDTO(Users user) {
        this(user.getId(), user.getEmail(), user.getCreateAt());
    }
}
