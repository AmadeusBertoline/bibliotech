package com.bibliotech.bibliotech.controller;

import com.bibliotech.bibliotech.dto.LoanFilterStatus;
import com.bibliotech.bibliotech.dto.LoanRequest;
import com.bibliotech.bibliotech.dto.LoanResponse;
import com.bibliotech.bibliotech.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;

@Tag(name = "Empréstimos", description = "Criação, devolução e renovação de empréstimos")
@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(summary = "Realiza um novo empréstimo", description = "Cria um registro de empréstimo de livro para um membro")
    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody LoanRequest request) {
        LoanResponse created = loanService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Registra a devolução de um empréstimo", description = "Finaliza o empréstimo com a devolução do livro")
    @PostMapping("/{id}/return")
    public LoanResponse returnLoan(@PathVariable Long id) {
        return loanService.returnLoan(id);
    }

    @Operation(summary = "Renova um empréstimo", description = "Estende o prazo de entrega de um empréstimo ativo")
    @PostMapping("/{id}/renew")
    public LoanResponse renew(@PathVariable Long id) {
        return loanService.renew(id);
    }

    @Operation(summary = "Busca um empréstimo por ID", description = "Retorna os detalhes de um empréstimo específico com base no ID informado")
    @GetMapping("/{id}")
    public LoanResponse findById(@PathVariable Long id) {
        return loanService.findById(id);
    }

    @Operation(summary = "Lista ou filtra os empréstimos", description = "Permite filtrar por status ou ID do membro com suporte a paginação")
    @GetMapping
    public Page<LoanResponse> search(
            @RequestParam(required = false) LoanFilterStatus status,
            @RequestParam(required = false) Long memberId,
            @PageableDefault(size = 20, sort = "loanDate") Pageable pageable) {
        return loanService.search(status, memberId, pageable);
    }

    @Operation(summary = "Lista empréstimos em atraso", description = "Retorna uma página com todos os empréstimos cuja data de vencimento já passou")
    @GetMapping("/overdue")
    public Page<LoanResponse> overdue(@PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return loanService.findOverdue(pageable);
    }
}