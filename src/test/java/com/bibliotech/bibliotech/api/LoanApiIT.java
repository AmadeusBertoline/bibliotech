package com.bibliotech.bibliotech.api;

import com.bibliotech.bibliotech.domain.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoanApiIT extends AbstractApiIT {

    @Test
    void createLoan_returns201_decrementsStock_andSetsFourteenDayDueDate() throws Exception {
        long bookId = createBook(3);
        long memberId = createMember(MemberStatus.ACTIVE);

        send(loanRequest(bookId, memberId), librarianToken())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.dueDate").value(today().plusDays(14).toString()))
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.fineAmount").value(0));

        assertThat(availableCopies(bookId)).isEqualTo(2);
    }

    @Test
    void createLoan_lastCopyGoesToFirstMember_secondGets422() throws Exception {
        long bookId = createBook(1);
        openLoan(bookId, createMember(MemberStatus.ACTIVE));

        send(loanRequest(bookId, createMember(MemberStatus.ACTIVE)), librarianToken())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("exemplares")));
    }

    @Test
    void createLoan_unknownBookOrMember_returns404() throws Exception {
        long memberId = createMember(MemberStatus.ACTIVE);
        long bookId = createBook(1);

        send(loanRequest(999999, memberId), librarianToken()).andExpect(status().isNotFound());
        send(loanRequest(bookId, 999999), librarianToken()).andExpect(status().isNotFound());
        assertThat(availableCopies(bookId)).isEqualTo(1);
    }

    @Test
    void createLoan_withMissingFields_returns400() throws Exception {
        send(jsonRequest(HttpMethod.POST, "/loans", Map.of()), librarianToken())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLoan_rejectsFourthActiveLoanForSameMember() throws Exception {
        long memberId = createMember(MemberStatus.ACTIVE);
        for (int i = 0; i < 3; i++) {
            openLoan(createBook(1), memberId);
        }

        send(loanRequest(createBook(1), memberId), librarianToken())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("limite")));
    }

    @Test
    void createLoan_rejectsSuspendedMember() throws Exception {
        long bookId = createBook(1);

        send(loanRequest(bookId, createMember(MemberStatus.SUSPENDED)), librarianToken())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("suspenso")));
        assertThat(availableCopies(bookId)).isEqualTo(1);
    }

    @Test
    void createLoan_rejectsSameBookTwiceForSameMember() throws Exception {
        long bookId = createBook(3);
        long memberId = createMember(MemberStatus.ACTIVE);
        openLoan(bookId, memberId);

        send(loanRequest(bookId, memberId), librarianToken()).andExpect(status().isUnprocessableContent());
    }

    @Test
    void returnLoan_restoresStock_andSecondReturnIs422() throws Exception {
        long bookId = createBook(1);
        long loanId = openLoan(bookId, createMember(MemberStatus.ACTIVE));

        send(post("/loans/{id}/return", loanId), librarianToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").value(today().toString()))
                .andExpect(jsonPath("$.fineAmount").value(0));
        assertThat(availableCopies(bookId)).isEqualTo(1);

        send(post("/loans/{id}/return", loanId), librarianToken()).andExpect(status().isUnprocessableContent());
        assertThat(availableCopies(bookId)).isEqualTo(1);   // não duplicou estoque
    }

    @Test
    void renew_extendsSevenDays_onlyOnce() throws Exception {
        long loanId = openLoan(createBook(1), createMember(MemberStatus.ACTIVE));

        send(post("/loans/{id}/renew", loanId), librarianToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value(today().plusDays(21).toString()))
                .andExpect(jsonPath("$.renewals").value(1));

        send(post("/loans/{id}/renew", loanId), librarianToken())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("limite")));
    }

    @Test
    void overdueLoan_accruesFine_blocksRenewalAndNewLoans_thenFineIsChargedOnReturn() throws Exception {
        long memberId = createMember(MemberStatus.ACTIVE);
        long bookId = createBook(2);
        long loanId = openLoan(bookId, memberId);
        makeOverdue(loanId, 5);

        // aparece como atrasado, com multa acumulada de 5 x R$ 0,50
        send(get("/loans/overdue"), librarianToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].overdue").value(true))
                .andExpect(jsonPath("$.content[0].fineAmount").value(2.5));
        send(get("/loans?status=OVERDUE"), librarianToken())
                .andExpect(jsonPath("$.content.length()").value(1));

        // não renova e o membro não pega outro livro
        send(post("/loans/{id}/renew", loanId), librarianToken()).andExpect(status().isUnprocessableContent());
        send(loanRequest(createBook(1), memberId), librarianToken())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("atraso")));

        // devolução cobra a multa e libera o membro
        send(post("/loans/{id}/return", loanId), librarianToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fineAmount").value(2.5));
        send(get("/loans/overdue"), librarianToken()).andExpect(jsonPath("$.content.length()").value(0));
        openLoan(createBook(1), memberId);
    }

    @Test
    void list_filtersByStatus_andRejectsInvalidStatus() throws Exception {
        long memberId = createMember(MemberStatus.ACTIVE);
        long returned = openLoan(createBook(1), memberId);
        openLoan(createBook(1), memberId);
        send(post("/loans/{id}/return", returned), librarianToken()).andExpect(status().isOk());

        send(get("/loans?status=ACTIVE&memberId=" + memberId), librarianToken())
                .andExpect(jsonPath("$.content.length()").value(1));
        send(get("/loans?status=RETURNED&memberId=" + memberId), librarianToken())
                .andExpect(jsonPath("$.content.length()").value(1));
        send(get("/loans?status=XYZ"), librarianToken()).andExpect(status().isBadRequest());
    }

    @Test
    void deleteBook_withActiveLoan_returns422_andKeepsBook() throws Exception {
        long bookId = createBook(1);
        openLoan(bookId, createMember(MemberStatus.ACTIVE));

        send(delete("/books/{id}", bookId), adminToken()).andExpect(status().isUnprocessableContent());
        send(get("/books/{id}", bookId), librarianToken()).andExpect(status().isOk());
    }

    /** Recua as datas direto no banco para simular atraso sem esperar 14 dias. */
    private void makeOverdue(long loanId, int daysLate) {
        LocalDate due = today().minusDays(daysLate);
        jdbc.update("UPDATE loans SET loan_date = ?, due_date = ? WHERE id = ?", due.minusDays(14), due, loanId);
    }
}