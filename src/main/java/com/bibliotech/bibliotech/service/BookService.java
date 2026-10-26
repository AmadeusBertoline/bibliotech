package com.bibliotech.bibliotech.service;

import com.bibliotech.bibliotech.domain.Book;
import com.bibliotech.bibliotech.domain.LoanStatus;
import com.bibliotech.bibliotech.dto.BookRequest;
import com.bibliotech.bibliotech.dto.BookResponse;
import com.bibliotech.bibliotech.exception.BusinessRuleException;
import com.bibliotech.bibliotech.exception.ConflictException;
import com.bibliotech.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.bibliotech.respository.BookRepository;
import com.bibliotech.bibliotech.respository.LoanRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.bibliotech.bibliotech.respository.BookSpecifications.authorContains;
import static com.bibliotech.bibliotech.respository.BookSpecifications.availableOnly;
import static com.bibliotech.bibliotech.respository.BookSpecifications.titleContains;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookService(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    public Page<BookResponse> search(String title, String author, Boolean available, Pageable pageable) {
        Specification<Book> spec = Specification.allOf(
                titleContains(title),
                authorContains(author),
                availableOnly(available));
        return bookRepository.findAll(spec, pageable).map(BookResponse::from);
    }

    @Cacheable(cacheNames = "books", key = "#id")
    public BookResponse findById(Long id) {
        return BookResponse.from(getOrThrow(id));
    }

    @Transactional
    public BookResponse create(BookRequest req) {
        if (bookRepository.existsByIsbn(req.isbn())) {
            throw new ConflictException("Já existe um livro com o ISBN " + req.isbn());
        }
        Book book = new Book();
        apply(book, req);
        book.setAvailableCopies(req.totalCopies());
        return BookResponse.from(bookRepository.save(book));
    }

    @CacheEvict(cacheNames = "books", key = "#id")
    @Transactional
    public BookResponse update(Long id, BookRequest req) {
        Book book = getOrThrow(id);

        if (!book.getIsbn().equals(req.isbn()) && bookRepository.existsByIsbn(req.isbn())) {
            throw new ConflictException("Já existe um livro com o ISBN " + req.isbn());
        }

        // Ajusta disponíveis pela diferença do total, sem deixar ficar negativo
        int delta = req.totalCopies() - book.getTotalCopies();
        int newAvailable = book.getAvailableCopies() + delta;
        if (newAvailable < 0) {
            throw new BusinessRuleException(
                    "Não é possível reduzir o total para %d: há exemplares emprestados".formatted(req.totalCopies()));
        }

        apply(book, req);
        book.setAvailableCopies(newAvailable);
        return BookResponse.from(bookRepository.save(book));
    }

    @CacheEvict(cacheNames = "books", key = "#id")
    @Transactional
    public void delete(Long id) {
        Book book = getOrThrow(id);
        if (loanRepository.existsByBookIdAndStatus(id, LoanStatus.ACTIVE)) {
            throw new BusinessRuleException("Não é possível excluir um livro com empréstimo ativo");
        }
        bookRepository.delete(book);
    }

    private Book getOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro", id));
    }

    private void apply(Book book, BookRequest req) {
        book.setTitle(req.title());
        book.setIsbn(req.isbn());
        book.setAuthor(req.author());
        book.setPublishedYear(req.publishedYear());
        book.setTotalCopies(req.totalCopies());
    }
}