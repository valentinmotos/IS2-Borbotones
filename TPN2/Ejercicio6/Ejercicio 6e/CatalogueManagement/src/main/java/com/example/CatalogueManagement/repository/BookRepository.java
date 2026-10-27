package com.example.CatalogueManagement.repository;

import com.example.CatalogueManagement.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
