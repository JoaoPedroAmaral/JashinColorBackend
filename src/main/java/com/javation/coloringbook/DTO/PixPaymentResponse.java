package com.javation.coloringbook.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PixPaymentResponse {
    private Long   mpPaymentId;   // ID do pagamento no Mercado Pago
    private String pixCopyPaste;  // Código copia-e-cola
    private String qrCodeImage;   // data:image/png;base64,...
}
