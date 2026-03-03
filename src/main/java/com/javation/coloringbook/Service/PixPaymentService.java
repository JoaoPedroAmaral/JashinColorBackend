package com.javation.coloringbook.Service;

import com.javation.coloringbook.DTO.PixPaymentResponse;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PixPaymentService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    public PixPaymentResponse generatePixQrCode(Double amount, String bookTitle) {
        try {
            // Seta o token diretamente antes de cada chamada
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("Usando token: {}", accessToken.substring(0, 15) + "...");

            PaymentClient client = new PaymentClient();

            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .transactionAmount(BigDecimal.valueOf(amount))
                    .description(bookTitle)
                    .paymentMethodId("pix")
                    .payer(PaymentPayerRequest.builder()
                            .email("test_user_" + System.currentTimeMillis() + "@testuser.com")
                            .firstName("Test")
                            .lastName("User")
                            .build())
                    .build();

            Payment payment = client.create(request);

            log.info("PIX gerado com sucesso. Payment ID: {}", payment.getId());

            // O MP retorna o QR Code em texto e em base64
            String qrCode     = payment.getPointOfInteraction().getTransactionData().getQrCode();
            String qrCodeB64  = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();

            return new PixPaymentResponse(
                    payment.getId(),         // ID do pagamento no MP
                    qrCode,                  // Copia-e-cola PIX
                    "data:image/png;base64," + qrCodeB64  // imagem pronta
            );

        } catch (MPApiException e) {
            log.error("Erro API Mercado Pago: status={} | mensagem={}",
                    e.getStatusCode(),
                    e.getApiResponse().getContent()); // <-- isso tem que aparecer
            throw new RuntimeException("Erro ao gerar PIX: " + e.getMessage());

        } catch (MPException e) {
            log.error("Erro SDK Mercado Pago: {}", e.getMessage());
            throw new RuntimeException("Erro ao gerar PIX: " + e.getMessage());
        }
    }
}