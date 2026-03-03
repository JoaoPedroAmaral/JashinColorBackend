package com.javation.coloringbook.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutResponseDTO {
    private String preferenceId;
    private String initPoint;        // URL produção
    private String sandboxInitPoint; // URL teste — use essa agora
}