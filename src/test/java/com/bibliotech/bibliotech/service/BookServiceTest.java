package com.bibliotech.bibliotech.service;

import com.bibliotech.bibliotech.domain.Book;
import com.bibliotech.bibliotech.domain.LoanStatus;
import com.bibliotech.bibliotech.dto.BookRequest;
import com.bibliotech.bibliotech.dto.BookResponse;
import com.bibliotech.bibliotech.exception.BusinessRuleException;
import com.bibliotech.bibliotech.exception.ConflictException;
import com.bibliotech.bibliotech.respository.BookRepository;
import com.bibliotech.bibliotech.respository.LoanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final String ISBN = "978-0134685991";

    @Mock BookRepository bookRepository;
    @Mock LoanRepository loanRepository;
    @InjectMocks BookService service;

    @Test
    @DisplayName("livro novo nasce com todos os exemplares disponíveis")
    void create_startsWithAllCopiesAvailable() {
        given(bookRepository.existsByIsbn(ISBN)).willReturn(false);
        given(bookRepository.save(any(Book.class))).willAnswer(returnsFirstArg());

        BookResponse response = service.create(request(3));

        assertThat(response.totalCopies()).isEqualTo(3);
        assertThat(response.availableCopies()).isEqualTo(3);
    }

    @Test
    void create_rejectsDuplicateIsbn() {
        given(bookRepository.existsByIsbn(ISBN)).willReturn(true);

        assertThatThrownBy(() -> service.create(request(3))).isInstanceOf(ConflictException.class);
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("aumentar o total aumenta os disponíveis na mesma proporção")
    void update_growingTotalRaisesAvailable() {
        Book book = book(3, 1);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(bookRepository.save(any(Book.class))).willAnswer(returnsFirstArg());

        BookResponse response = service.update(1L, request(5));

        assertThat(response.totalCopies()).isEqualTo(5);
        assertThat(response.availableCopies()).isEqualTo(3);
    }

    @Test
    @DisplayName("não permite reduzir o total abaixo dos exemplares emprestados")
    void update_rejectsShrinkingBelowLentCopies() {
        Book book = book(3, 1);     // 2 emprestados
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));

        assertThatThrownBy(() -> service.update(1L, request(1)))
                .isInstanceOf(BusinessRuleException.class);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void delete_rejectsBookWithActiveLoan() {
        Book book = book(3, 2);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));
        given(loanRepository.existsByBookIdAndStatus(1L, LoanStatus.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessRuleException.class);
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    void delete_removesBookWithoutActiveLoans() {
        Book book = book(3, 3);
        given(bookRepository.findById(1L)).willReturn(Optional.of(book));

        service.delete(1L);

        verify(bookRepository).delete(book);
    }

    private static BookRequest request(int totalCopies) {
        return new BookRequest("Effective Java", ISBN, "Joshua Bloch", 2018, totalCopies);
    }

    private static Book book(int total, int available) {
        Book b = new Book();
        b.setId(1L);
        b.setTitle("Effective Java");
        b.setIsbn(ISBN);
        b.setAuthor("Joshua Bloch");
        b.setPublishedYear(2018);
        b.setTotalCopies(total);
        b.setAvailableCopies(available);
        return b;
    }
}