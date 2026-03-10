package com.javation.coloringbook.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequest {

    @NotNull(message = "Lista de arquivos não pode ser nula")
    @NotEmpty(message = "Lista de arquivos não pode estar vazia")
    private List<MultipartFile> files;

    @Min(value = 1, message = "Deve ter pelo menos 1 página")
    @Max(value = 50, message = "Não pode exceder 50 páginas")
    private Integer expectedPages;

    @NotNull(message = "O preço não pode ser nulo")
    @Min(value = 1, message = "O preço deve ser pelo menos 1")
    private Double price;
}
