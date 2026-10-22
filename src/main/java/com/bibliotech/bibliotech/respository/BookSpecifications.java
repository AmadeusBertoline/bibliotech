package com.bibliotech.bibliotech.respository;

import com.bibliotech.bibliotech.domain.Book;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecifications {

    private BookSpecifications() {}

    public static Specification<Book> titleContains(String title) {
        return (root, query, cb) -> title == null || title.isBlank()
                ? null
                : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Book> authorContains(String author) {
        return (root, query, cb) -> author == null || author.isBlank()
                ? null
                : cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%");
    }

    public static Specification<Book> availableOnly(Boolean available) {
        return (root, query, cb) -> available == null
                ? null
                : available
                    ? cb.greaterThan(root.get("availableCopies"), 0)
                    : cb.equal(root.get("availableCopies"), 0);
    }
}