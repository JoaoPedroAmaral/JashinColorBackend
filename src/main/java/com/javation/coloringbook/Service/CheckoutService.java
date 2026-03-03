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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    private final BooksRepository booksRepository;

    public CheckoutResponseDTO createPreference(Long bookId) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("Criando preferência para o livro ID: {}", bookId);

            Books book = booksRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(book.getId().toString())
                    .title(book.getTitle())
                    .description("Livro de colorir: " + book.getTitle())
                    .quantity(1)
                    .currencyId("BRL")
                    .unitPrice(new BigDecimal("29.90")) // ou pega do book se tiver campo de preço
                    .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(item);

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(PreferenceBackUrlsRequest.builder()
                            .success("https://www.google.com") // temporário para testar
                            .failure("https://www.google.com")
                            .pending("https://www.google.com")
                            .build())
                    // .autoReturn("approved")  <-- remove essa linha
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            log.info("Preferência criada com sucesso. ID: {}", preference.getId());

            return new CheckoutResponseDTO(
                    preference.getId(),
                    preference.getInitPoint(),      // URL de pagamento produção
                    preference.getSandboxInitPoint() // URL de pagamento sandbox (use essa para testar)
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