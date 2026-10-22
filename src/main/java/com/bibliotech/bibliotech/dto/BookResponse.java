package com.bibliotech.bibliotech.dto;

import com.bibliotech.bibliotech.domain.Book;

public record BookResponse(
        Long id,
        String title,
        String isbn,
        String author,
        Integer publishedYear,
        Integer totalCopies,
        Integer availableCopies
) {
    public static BookResponse from(Book b) {
        return new BookResponse(
                b.getId(), b.getTitle(), b.getIsbn(), b.getAuthor(),
                b.getPublishedYear(), b.getTotalCopies(), b.getAvailableCopies());
    }
}