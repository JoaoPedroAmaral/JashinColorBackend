package com.javation.coloringbook.Controller;

import com.javation.coloringbook.DTO.PaymentResponseDTO;
import com.javation.coloringbook.Entity.Payment;
import com.javation.coloringbook.Service.PaymentService;
import com.javation.coloringbook.Service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService webhookService;

    @Value("${app.frontendUrl}")
    private String frontendUrl;

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
    public ResponseEntity<Void> handlePaymentSuccess(
            @RequestParam(value = "payment_id", required = false) String paymentId,
            @RequestParam(value = "collection_id", required = false) String collectionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "external_reference", required = false) String externalReference) {
        
        String id = (paymentId != null) ? paymentId : collectionId;
        log.info("User returned from payment: ID: {}, Status: {}, Ref: {}", id, status, externalReference);

        if ("approved".equals(status) && id != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("status", status);
            payload.put("external_reference", externalReference);
            
            webhookService.processNotification(id, "payment", payload);
        }

        // Garante que a frontendUrl não termine com /
        String cleanFrontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        
        // Redireciona para o seu frontend local (localhost:3000)
        String redirectUrl = cleanFrontendUrl + "/payment-success?status=" + status + "&bookId=" + externalReference;
        
        log.info("Redirecting user back to frontend: {}", redirectUrl);
        return ResponseEntity.status(302)
                .location(URI.create(redirectUrl))
                .build();
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
