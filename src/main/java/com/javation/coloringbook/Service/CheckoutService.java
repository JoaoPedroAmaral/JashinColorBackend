package com.javation.coloringbook.Service;

import com.javation.coloringbook.DTO.CheckoutResponseDTO;
import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Repository.BooksRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${app.baseUrl}")
    private String baseUrl;

    @Value("${app.frontendUrl:http://localhost:3000}")
    private String frontendUrl;

    private final BooksRepository booksRepository;

    public CheckoutResponseDTO createPreference(Long bookId) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);

            // Garante que as URLs não terminem com /
            String cleanBaseUrl = (baseUrl == null || baseUrl.isEmpty()) ? "http://localhost:8080" : baseUrl;
            if (cleanBaseUrl.endsWith("/")) cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
            
            String cleanFrontendUrl = (frontendUrl == null || frontendUrl.isEmpty()) ? "http://localhost:3000" : frontendUrl;
            if (cleanFrontendUrl.endsWith("/")) cleanFrontendUrl = cleanFrontendUrl.substring(0, cleanFrontendUrl.length() - 1);

            String successUrl = cleanBaseUrl + "/api/payments/success";
            log.info("Configuring MP Preference - Success URL: {}", successUrl);

            Books book = booksRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(book.getId().toString())
                    .title(book.getTitle())
                    .description("Livro de colorir: " + book.getTitle())
                    .quantity(1)
                    .currencyId("BRL")
                    .unitPrice(BigDecimal.valueOf(book.getPrice()))
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Collections.singletonList(item))
                    .backUrls(PreferenceBackUrlsRequest.builder()
                            .success(successUrl)
                            .failure(cleanFrontendUrl + "/payment-failure")
                            .pending(cleanFrontendUrl + "/payment-pending")
                            .build())
                    .autoReturn("approved")
                    .notificationUrl(cleanBaseUrl + "/api/payments/webhook")
                    .externalReference(book.getId().toString())
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            return new CheckoutResponseDTO(
                    preference.getId(),
                    preference.getInitPoint(),
                    preference.getSandboxInitPoint()
            );

        } catch (MPApiException e) {
            log.error("Erro API MP: status={} | mensagem={}", e.getStatusCode(), e.getApiResponse().getContent());
            throw new RuntimeException("Erro ao criar preferência: " + e.getMessage());
        } catch (MPException e) {
            log.error("Erro SDK MP: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar preferência: " + e.getMessage());
        }
    }
}
