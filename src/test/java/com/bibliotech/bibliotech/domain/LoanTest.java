package com.bibliotech.bibliotech.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LoanTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private static Loan openedOn(LocalDate date) {
        return Loan.open(new Book(), new Member(), date);
    }

    @Test
    @DisplayName("prazo padrão é de 14 dias")
    void open_setsFourteenDayDueDate() {
        Loan loan = openedOn(TODAY);

        assertThat(loan.getLoanDate()).isEqualTo(TODAY);
        assertThat(loan.getDueDate()).isEqualTo(TODAY.plusDays(14));
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getRenewals()).isZero();
    }

    @ParameterizedTest(name = "{0} dia(s) de atraso -> multa de R$ {1}")
    @CsvSource({"0,0.00", "1,0.50", "5,2.50", "30,15.00"})
    void fine_isFiftyCentsPerDayLate(int daysLate, String expected) {
        Loan loan = openedOn(TODAY.minusDays(14 + daysLate));
        loan.close(TODAY);

        assertThat(loan.fine(TODAY)).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("devolução antes do vencimento não gera multa")
    void fine_isZeroWhenReturnedEarly() {
        Loan loan = openedOn(TODAY.minusDays(10));
        loan.close(TODAY);

        assertThat(loan.fine(TODAY)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("empréstimo ativo e atrasado acumula multa até hoje")
    void fine_accruesWhileActive() {
        Loan loan = openedOn(TODAY.minusDays(17));   // venceu há 3 dias

        assertThat(loan.fine(TODAY)).isEqualByComparingTo("1.50");
    }

    @Test
    @DisplayName("depois da devolução a multa congela, não cresce com o tempo")
    void fine_stopsGrowingAfterReturn() {
        Loan loan = openedOn(TODAY.minusDays(17));   // venceu há 3 dias
        loan.close(TODAY.minusDays(1));              // devolvido com 2 dias de atraso

        assertThat(loan.fine(TODAY.plusDays(10))).isEqualByComparingTo("1.00");
    }

    @Test
    void isOverdue_dueDateTodayIsNotOverdue_yesterdayIs() {
        assertThat(openedOn(TODAY.minusDays(14)).isOverdue(TODAY)).isFalse();
        assertThat(openedOn(TODAY.minusDays(15)).isOverdue(TODAY)).isTrue();
    }

    @Test
    void isOverdue_returnedLoanIsNeverOverdue() {
        Loan loan = openedOn(TODAY.minusDays(30));
        loan.close(TODAY);

        assertThat(loan.isOverdue(TODAY)).isFalse();
    }

    @Test
    @DisplayName("renovação soma 7 dias e só pode acontecer uma vez")
    void renew_extendsSevenDays_andAllowsOnlyOnce() {
        Loan loan = openedOn(TODAY);
        assertThat(loan.canRenew()).isTrue();

        loan.renew();

        assertThat(loan.getDueDate()).isEqualTo(TODAY.plusDays(21));
        assertThat(loan.getRenewals()).isEqualTo(1);
        assertThat(loan.canRenew()).isFalse();
    }
}