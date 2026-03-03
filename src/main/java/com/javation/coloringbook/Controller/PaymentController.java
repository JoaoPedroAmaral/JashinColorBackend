package com.javation.coloringbook.Controller;

import com.javation.coloringbook.DTO.PaymentResponseDTO;
import com.javation.coloringbook.Entity.Payment;
import com.javation.coloringbook.Service.PaymentService;
import com.javation.coloringbook.Service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService webhookService;

    @PostMapping("/register")
    public ResponseEntity<?> registerPayment(@RequestParam Long bookPayId, @RequestParam String transactionId) {
        try {
            Payment payment = paymentService.registerPayment(bookPayId, transactionId);
            return ResponseEntity.ok(new PaymentResponseDTO(payment));
        } catch (Exception e) {
            log.error("Error registering payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/success")
    public ResponseEntity<String> handlePaymentSuccess(
            @RequestParam(value = "payment_id", required = false) String paymentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "external_reference", required = false) String externalReference) {
        
        log.info("User returned from payment: ID: {}, Status: {}, Ref: {}", paymentId, status, externalReference);

        if ("approved".equals(status) && paymentId != null) {
            webhookService.processNotification(paymentId, "payment", null);
            return ResponseEntity.ok("Pagamento aprovado!");
        }
        
        return ResponseEntity.ok("Seu pagamento está sendo processado.");
    }

    @GetMapping("/book/{bookPayId}")
    public ResponseEntity<?> getPaymentByBookId(@PathVariable Long bookPayId) {
        try {
            Payment payment = paymentService.getPaymentsByBookId(bookPayId);
            return ResponseEntity.ok(new PaymentResponseDTO(payment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
