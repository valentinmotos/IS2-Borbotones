package com.payu.CatalogueManagement.service;

import com.payu.CatalogueManagement.entity.Book;
import com.payu.CatalogueManagement.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        validar(book);
        book.setId(null);
        return repository.save(book);
    }

    public Book update(Long id, Book book) {
        findById(id);
        validar(book);
        book.setId(id);
        return repository.save(book);
    }

    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }

    public Book findById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro no encontrado"));
    }

    private void validar(Book book) {
        if (book.getName() == null || book.getName().isBlank() || book.getName().length() > 255
                || book.getIsbnNumber() == null || book.getIsbnNumber().isBlank() || book.getIsbnNumber().length() > 255
                || book.getPublishDate() == null || book.getBookType() == null
                || book.getPrice() == null || !Double.isFinite(book.getPrice()) || book.getPrice() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Complete titulo, ISBN, fecha, formato y un precio valido mayor o igual a cero");
        }
        book.setName(book.getName().trim());
        book.setIsbnNumber(book.getIsbnNumber().trim());
    }
}
