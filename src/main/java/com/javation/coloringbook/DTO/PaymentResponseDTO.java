package com.javation.coloringbook.DTO;

import com.javation.coloringbook.Entity.Payment;
import com.javation.coloringbook.Entity.TransactionStatus;
import lombok.Getter;

@Getter
public class PaymentResponseDTO {
    private Long id;
    private Long bookPayId;
    private String transationId;
    private String status;


    public PaymentResponseDTO(Payment payment){
        this.id = payment.getId();
        this.bookPayId = payment.getBookPay().getId();
        this.transationId = payment.getTransactionId();
        this.status = payment.getStatusPay().name();
    }
}
