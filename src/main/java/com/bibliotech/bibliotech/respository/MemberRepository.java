package com.bibliotech.bibliotech.respository;

import com.bibliotech.bibliotech.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
}