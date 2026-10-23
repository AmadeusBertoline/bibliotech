package com.bibliotech.bibliotech.api;

import com.bibliotech.bibliotech.domain.MemberStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class LoanConcurrencyIT extends AbstractApiIT {

    @Test
    void lastCopy_isLentToExactlyOneMember_whenRequestsRaceEachOther() throws Exception {
        int threads = 8;
        long bookId = createBook(1);
        List<Long> members = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            members.add(createMember(MemberStatus.ACTIVE));
        }
        String token = librarianToken();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (Long memberId : members) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();      // todas as threads disparam juntas
                return send(loanRequest(bookId, memberId), token).andReturn().getResponse().getStatus();
            }));
        }
        ready.await();
        go.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 422).hasSize(threads - 1);
        assertThat(availableCopies(bookId)).isZero();
    }
}