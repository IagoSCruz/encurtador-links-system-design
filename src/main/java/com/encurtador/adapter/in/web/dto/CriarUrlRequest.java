package com.encurtador.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para criação de URL encurtada.
 * Apenas valida que o campo não está em branco (→ 400).
 * A validação de formato (http/https) é feita no domínio (→ 422).
 */
public record CriarUrlRequest(
                @NotBlank(message = "urlOriginal é obrigatória") String urlOriginal) {
}
