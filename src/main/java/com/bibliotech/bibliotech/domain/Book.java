package com.bibliotech.bibliotech.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(name = "published_year", nullable = false)
    private Integer publishedYear;

    @Column(name = "total_copies", nullable = false)
    private Integer totalCopies;

    @Column(name = "available_copies", nullable = false)
    private Integer availableCopies;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void decrementAvailable() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("Sem exemplares disponíveis");
        }
        availableCopies--;
    }

    public void incrementAvailable() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("Disponíveis não pode exceder o total");
        }
        availableCopies++;
    }
}