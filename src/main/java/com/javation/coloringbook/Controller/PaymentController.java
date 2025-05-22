package com.javation.coloringbook.Controller;

import com.javation.coloringbook.DTO.PaymentResponseDTO;
import com.javation.coloringbook.Entity.Payment;
import com.javation.coloringbook.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/register")
    public ResponseEntity<?> registerPayment(@RequestParam Long bookPayId, @RequestParam String transactionId){
        try {
            Payment payment = paymentService.registerPayment(bookPayId,transactionId);
            return ResponseEntity.ok(new PaymentResponseDTO(payment));
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{payId}/pay")
    public ResponseEntity<String> confirmedPayment(@PathVariable Long payId){
        paymentService.confirmPayment(payId);
        return ResponseEntity.ok("Pagamento efetuado com sucesso!");
    }

    @GetMapping("/book/{bookPayId}")
    public ResponseEntity<?> getPaymentByBookId (@PathVariable Long bookPayId){
        Payment payment = paymentService.getPaymentsByBookId(bookPayId);
        return ResponseEntity.ok(new PaymentResponseDTO(payment));
    }
}
