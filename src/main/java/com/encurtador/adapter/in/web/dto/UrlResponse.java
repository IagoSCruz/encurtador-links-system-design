package com.encurtador.adapter.in.web.dto;

import com.encurtador.domain.model.UrlEncurtada;

/**
 * DTO de resposta — nunca expõe o record de domínio diretamente.
 */
public record UrlResponse(
        String codigo,
        String urlCurta,
        String urlOriginal) {
    public static UrlResponse from(UrlEncurtada urlEncurtada, String baseUrl) {
        return new UrlResponse(
                urlEncurtada.codigo(),
                baseUrl + "/" + urlEncurtada.codigo(),
                urlEncurtada.urlOriginal());
    }
}
