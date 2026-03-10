package com.javation.coloringbook.Controller;

import com.javation.coloringbook.DTO.CheckoutResponseDTO;
import com.javation.coloringbook.Service.CheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/preference")
    public ResponseEntity<?> createPreference(@RequestParam Long bookId) {
        try {
            CheckoutResponseDTO response = checkoutService.createPreference(bookId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao criar preferência: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
