package com.encurtador.adapter.in.web;

import com.encurtador.adapter.in.web.dto.CriarUrlRequest;
import com.encurtador.adapter.in.web.dto.UrlResponse;
import com.encurtador.application.port.in.CriarUrlUseCase;
import com.encurtador.application.port.in.RedirecionarUrlUseCase;
import com.encurtador.domain.exception.UrlInvalidaException;
import com.encurtador.domain.exception.UrlNaoEncontradaException;
import com.encurtador.domain.model.UrlEncurtada;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@Tag(name = "URLs", description = "Encurtamento e redirecionamento de URLs")
public class UrlController {

    private final CriarUrlUseCase criarUrlUseCase;
    private final RedirecionarUrlUseCase redirecionarUrlUseCase;

    public UrlController(CriarUrlUseCase criarUrlUseCase,
            RedirecionarUrlUseCase redirecionarUrlUseCase) {
        this.criarUrlUseCase = criarUrlUseCase;
        this.redirecionarUrlUseCase = redirecionarUrlUseCase;
    }

    @Operation(summary = "Criar URL encurtada", description = "Recebe uma URL original e retorna um código curto em Base62. "
            +
            "A URL é armazenada no PostgreSQL (primary) e indexada no Redis com TTL de 24h.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "URL encurtada com sucesso", content = @Content(schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "Body inválido ou campos obrigatórios ausentes", content = @Content(schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "422", description = "URL com formato inválido ou esquema não suportado", content = @Content(schema = @Schema(type = "string")))
    })
    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> criar(
            @Valid @RequestBody CriarUrlRequest request,
            HttpServletRequest httpRequest) {

        UrlEncurtada urlEncurtada = criarUrlUseCase.criar(request.urlOriginal());
        String baseUrl = extrairBaseUrl(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UrlResponse.from(urlEncurtada, baseUrl));
    }

    @Operation(summary = "Redirecionar para URL original", description = "Busca a URL original pelo código curto. " +
            "Consulta Redis primeiro (Cache-Aside); em cache miss, lê de uma réplica PostgreSQL (round-robin).")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirecionamento para a URL original"),
            @ApiResponse(responseCode = "404", description = "Código não encontrado ou URL expirada", content = @Content(schema = @Schema(type = "string")))
    })
    @GetMapping("/{codigo}")
    public ResponseEntity<Void> redirecionar(@PathVariable String codigo) {
        UrlEncurtada urlEncurtada = redirecionarUrlUseCase.buscarPorCodigo(codigo);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(urlEncurtada.urlOriginal()))
                .build();
    }

    @ExceptionHandler(UrlNaoEncontradaException.class)
    public ResponseEntity<String> handleNaoEncontrada(UrlNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UrlInvalidaException.class)
    public ResponseEntity<String> handleInvalida(UrlInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }

    private String extrairBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                        ? ":" + request.getServerPort()
                        : "");
    }
}
