package com.example.CatalogueManagement.service;

import com.example.CatalogueManagement.entity.Book;
import com.example.CatalogueManagement.repository.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
    }

    public Book create(Book book) {
        book.setId(null);
        return bookRepository.save(book);
    }

    public Book update(Long id, Book book) {
        Book existing = findById(id);
        existing.setTitle(book.getTitle());
        existing.setAuthor(book.getAuthor());
        existing.setIsbn(book.getIsbn());
        existing.setPublishedYear(book.getPublishedYear());
        return bookRepository.save(existing);
    }

    public void delete(Long id) {
        bookRepository.delete(findById(id));
    }
}
