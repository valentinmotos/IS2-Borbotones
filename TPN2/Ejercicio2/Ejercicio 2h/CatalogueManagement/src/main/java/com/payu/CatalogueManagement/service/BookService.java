package com.payu.CatalogueManagement.service;

import com.payu.CatalogueManagement.entity.Book;
import com.payu.CatalogueManagement.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BookService {

    private final BookRepository repository;

    @Autowired
    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> findAll() {
        return repository.findAll();
    }

    public Book save(Book book) {
        repository.findByIsbnNumberIgnoreCase(book.getIsbnNumber()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un libro con ese ISBN");
        });
        return repository.save(book);
    }

    public Book update(Long id, Book book) {
        Book existing = findById(id);
        repository.findByIsbnNumberIgnoreCase(book.getIsbnNumber())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un libro con ese ISBN");
                });
        existing.setName(book.getName());
        existing.setIsbnNumber(book.getIsbnNumber());
        existing.setPublishDate(book.getPublishDate());
        existing.setPrice(book.getPrice());
        existing.setBookType(book.getBookType());
        return repository.save(existing);
    }

    public Book findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un libro con ID " + id));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un libro con ID " + id);
        }
        repository.deleteById(id);
    }
}
