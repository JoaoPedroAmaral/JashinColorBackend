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

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    private final BooksRepository booksRepository;

    public CheckoutResponseDTO createPreference(Long bookId) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);

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
                            .success(baseUrl + "/api/payments/success")
                            .failure(baseUrl + "/api/payments/failure")
                            .pending(baseUrl + "/api/payments/pending")
                            .build())
                    .notificationUrl(baseUrl + "/api/payments/webhook")
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
