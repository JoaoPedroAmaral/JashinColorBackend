package com.javation.coloringbook.Controller;

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
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestBody(required = false) Map<String, Object> payload) {
        
        log.info("Webhook call received: Param ID: {}, Param Topic: {}, Body: {}", id, topic, payload);
        
        webhookService.processNotification(id, topic, payload);
        
        return ResponseEntity.ok().build();
    }
}
