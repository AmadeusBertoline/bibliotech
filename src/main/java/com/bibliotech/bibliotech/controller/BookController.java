package com.bibliotech.bibliotech.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.bibliotech.bibliotech.dto.BookRequest;
import com.bibliotech.bibliotech.dto.BookResponse;
import com.bibliotech.bibliotech.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;

@Tag(name = "Livros", description = "Gerenciamento e cadastro de livros")
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(summary = "Lista os livros", description = "Pode inserir parâmetros para filtrar por título, autor ou disponibilidade")
    @GetMapping
    public Page<BookResponse> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Boolean available,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return bookService.search(title, author, available, pageable);
    }

    @Operation(summary = "Busca um livro por ID", description = "Retorna os detalhes de um livro específico com base no ID informado")
    @GetMapping("/{id}")
    public BookResponse findById(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @Operation(summary = "Cadastra um novo livro", description = "Salva um novo livro no sistema com base nos dados fornecidos")
    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        BookResponse created = bookService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Atualiza um livro existente", description = "Atualiza os dados de um livro com base no ID e nas novas informações enviadas")
    @PutMapping("/{id}")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return bookService.update(id, request);
    }

    @Operation(summary = "Remove um livro", description = "Exclui um livro do sistema com base no ID informado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}