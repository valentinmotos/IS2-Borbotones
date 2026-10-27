package com.example.CatalogueManagement.service;

import com.example.CatalogueManagement.entity.Book;
import com.example.CatalogueManagement.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private final Book book = new Book(1L, "Ficciones", "Jorge Luis Borges", "978-84-206-3392-9", 1944);

    @Test
    void findAllReturnsAllBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(book));

        assertThat(bookService.findAll()).containsExactly(book);
    }

    @Test
    void findByIdReturnsBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThat(bookService.findById(1L).getTitle()).isEqualTo("Ficciones");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createIgnoresIncomingId() {
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book created = bookService.create(new Book(50L, "El Aleph", "Jorge Luis Borges", "978-84-206-3393-6", 1949));

        assertThat(created.getId()).isNull();
    }

    @Test
    void updateChangesFields() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book updated = bookService.update(1L, new Book(null, "Ficciones (2da ed.)", "Jorge Luis Borges", "978-84-206-3392-9", 1956));

        assertThat(updated.getTitle()).isEqualTo("Ficciones (2da ed.)");
        assertThat(updated.getPublishedYear()).isEqualTo(1956);
    }

    @Test
    void deleteRemovesBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.delete(1L);

        verify(bookRepository).delete(book);
    }
}
