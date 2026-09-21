package com.borbotones.biblioteca.repo;
import com.borbotones.biblioteca.model.Editorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
public interface EditorialRepository extends JpaRepository<Editorial, Long>, RevisionRepository<Editorial, Long, Integer> { }
