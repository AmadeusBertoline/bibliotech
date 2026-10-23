package com.bibliotech.bibliotech.controller;

import com.bibliotech.bibliotech.dto.LoanResponse;
import com.bibliotech.bibliotech.dto.MemberRequest;
import com.bibliotech.bibliotech.dto.MemberResponse;
import com.bibliotech.bibliotech.service.LoanService;
import com.bibliotech.bibliotech.service.MemberService;
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

@Tag(name = "Membros", description = "Gerenciamento de membros e associados da biblioteca")
@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final LoanService loanService;

    public MemberController(MemberService memberService, LoanService loanService) {
        this.memberService = memberService;
        this.loanService = loanService;
    }

    @Operation(summary = "Lista todos os membros", description = "Retorna uma página com todos os membros cadastrados ordenados por nome")
    @GetMapping
    public Page<MemberResponse> findAll(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return memberService.findAll(pageable);
    }

    @Operation(summary = "Busca um membro por ID", description = "Retorna os detalhes de um membro específico com base no ID informado")
    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return memberService.findById(id);
    }

    @Operation(summary = "Cadastra um novo membro", description = "Salva um novo membro no sistema com base nos dados fornecidos")
    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        MemberResponse created = memberService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Atualiza um membro existente", description = "Atualiza os dados de um membro com base no ID e nas novas informações")
    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return memberService.update(id, request);
    }

    @Operation(summary = "Lista empréstimos de um membro", description = "Retorna uma página com todos os empréstimos associados a um ID de membro específico")
    @GetMapping("/{id}/loans")
    public Page<LoanResponse> loans(@PathVariable Long id,
                                    @PageableDefault(size = 20, sort = "loanDate") Pageable pageable) {
        return loanService.findByMember(id, pageable);
    }
}