package com.bibliotech.bibliotech.service;

import com.bibliotech.bibliotech.domain.*;
import com.bibliotech.bibliotech.dto.LoanRequest;
import com.bibliotech.bibliotech.dto.LoanResponse;
import com.bibliotech.bibliotech.exception.BusinessRuleException;
import com.bibliotech.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.bibliotech.respository.BookRepository;
import com.bibliotech.bibliotech.respository.LoanRepository;
import com.bibliotech.bibliotech.respository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Mock LoanRepository loanRepository;
    @Mock BookRepository bookRepository;
    @Mock MemberRepository memberRepository;

    private LoanService service;
    private Book book;
    private Member member;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
        service = new LoanService(loanRepository, bookRepository, memberRepository, clock);

        book = new Book();
        book.setId(1L);
        book.setTitle("Effective Java");
        book.setTotalCopies(3);
        book.setAvailableCopies(2);     // um exemplar já emprestado

        member = new Member();
        member.setId(2L);
        member.setName("Ana Souza");
        member.setStatus(MemberStatus.ACTIVE);
    }

    // ---------- criação ----------

    @Test
    @DisplayName("empréstimo válido: prazo de 14 dias e baixa de um exemplar")
    void create_success() {
        stubBookAndMember();
        given(loanRepository.save(any(Loan.class))).willAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setId(10L);
            return l;
        });

        LoanResponse response = service.create(new LoanRequest(1L, 2L));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.loanDate()).isEqualTo(TODAY);
        assertThat(response.dueDate()).isEqualTo(TODAY.plusDays(14));
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(book.getAvailableCopies()).isEqualTo(1);
        verify(bookRepository).save(book);
    }

    @Test
    void create_bookNotFound() {
        given(bookRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new LoanRequest(1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void create_memberNotFound() {
        given(bookRepository.findByIdForUpdate(1L)).willReturn(Optional.of(book));
        given(memberRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new LoanRequest(1L, 2L)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void create_rejectsSuspendedMember() {
        stubBookAndMember();
        member.setStatus(MemberStatus.SUSPENDED);

        assertRejected("suspenso");
    }

    @Test
    void create_rejectsMemberWithOverdueLoan() {
        stubBookAndMember();
        given(loanRepository.existsByMemberIdAndStatusAndDueDateBefore(2L, LoanStatus.ACTIVE, TODAY))
                .willReturn(true);

        assertRejected("atraso");
    }

    @Test
    void create_rejectsMemberAtActiveLoanLimit() {
        stubBookAndMember();
        given(loanRepository.countByMemberIdAndStatus(2L, LoanStatus.ACTIVE)).willReturn(3L);

        assertRejected("limite");
    }

    @Test
    void create_rejectsSecondActiveLoanOfSameBook() {
        stubBookAndMember();
        given(loanRepository.existsByBookIdAndMemberIdAndStatus(1L, 2L, LoanStatus.ACTIVE)).willReturn(true);

        assertRejected("já possui");
    }

    @Test
    void create_rejectsWhenNoCopiesAvailable() {
        stubBookAndMember();
        book.setAvailableCopies(0);

        assertThatThrownBy(() -> service.create(new LoanRequest(1L, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exemplares");
        verify(loanRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
    }

    // ---------- devolução ----------

    @Test
    @DisplayName("devolução atrasada: 5 dias x R$ 0,50 = R$ 2,50 e exemplar volta ao estoque")
    void returnLoan_late_chargesFine() {
        Loan loan = activeLoan(TODAY.minusDays(19));      // venceu há 5 dias
        given(loanRepository.findDetailedById(10L)).willReturn(Optional.of(loan));
        given(bookRepository.findByIdForUpdate(1L)).willReturn(Optional.of(book));

        LoanResponse response = service.returnLoan(10L);

        assertThat(response.status()).isEqualTo(LoanStatus.RETURNED);
        assertThat(response.returnDate()).isEqualTo(TODAY);
        assertThat(response.fineAmount()).isEqualByComparingTo("2.50");
        assertThat(book.getAvailableCopies()).isEqualTo(3);
        verify(bookRepository).save(book);
        verify(loanRepository).save(loan);
    }

    @Test
    void returnLoan_onDueDate_hasNoFine() {
        Loan loan = activeLoan(TODAY.minusDays(14));      // vence hoje
        given(loanRepository.findDetailedById(10L)).willReturn(Optional.of(loan));
        given(bookRepository.findByIdForUpdate(1L)).willReturn(Optional.of(book));

        LoanResponse response = service.returnLoan(10L);

        assertThat(response.fineAmount()).isEqualByComparingTo("0");
    }

    @Test
    void returnLoan_alreadyReturned_isRejected() {
        Loan loan = activeLoan(TODAY.minusDays(5));
        loan.close(TODAY.minusDays(1));
        given(loanRepository.findDetailedById(10L)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.returnLoan(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("devolvido");
        verifyNoInteractions(bookRepository);
    }

    @Test
    void returnLoan_notFound() {
        given(loanRepository.findDetailedById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnLoan(10L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- renovação ----------

    @Test
    @DisplayName("renovação soma 7 dias ao vencimento")
    void renew_success() {
        Loan loan = activeLoan(TODAY.minusDays(3));       // vence em 11 dias
        given(loanRepository.findDetailedById(10L)).willReturn(Optional.of(loan));

        LoanResponse response = service.renew(10L);

        assertThat(response.dueDate()).isEqualTo(TODAY.plusDays(18));
        assertThat(response.renewals()).isEqualTo(1);
        verify(loanRepository).save(loan);
    }

    @Test
    void renew_rejectsOverdueLoan() {
        Loan loan = activeLoan(TODAY.minusDays(15));      // venceu ontem
        given(loanRepository.findDetailedById(10L)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.renew(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("atraso");
        verify(loanRepository, never()).save(any());
    }

    @Test
    void renew_rejectsSecondRenewal() {
        Loan loan = activeLoan(TODAY.minusDays(3));
        loan.renew();
        given(loanRepository.findDetailedById(10L)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.renew(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("limite");
    }

    @Test
    void renew_rejectsReturnedLoan() {
        Loan loan = activeLoan(TODAY.minusDays(3));
        loan.close(TODAY);
        given(loanRepository.findDetailedById(10L)).willReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.renew(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("devolvido");
    }

    // ---------- helpers ----------

    private void stubBookAndMember() {
        given(bookRepository.findByIdForUpdate(1L)).willReturn(Optional.of(book));
        given(memberRepository.findById(2L)).willReturn(Optional.of(member));
    }

    private Loan activeLoan(LocalDate loanDate) {
        Loan loan = Loan.open(book, member, loanDate);
        loan.setId(10L);
        return loan;
    }

    private void assertRejected(String messagePart) {
        assertThatThrownBy(() -> service.create(new LoanRequest(1L, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(messagePart);
        verify(loanRepository, never()).save(any());
        verify(bookRepository, never()).save(any());
        assertThat(book.getAvailableCopies()).isEqualTo(2);   // estoque intacto
    }
}