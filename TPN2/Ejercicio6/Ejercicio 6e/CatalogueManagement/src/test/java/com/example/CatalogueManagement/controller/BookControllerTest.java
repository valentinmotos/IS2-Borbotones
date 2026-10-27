package com.example.CatalogueManagement.controller;

import com.example.CatalogueManagement.entity.Book;
import com.example.CatalogueManagement.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
class BookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    private final Book book = new Book(1L, "Rayuela", "Julio Cortázar", "978-84-376-0494-7", 1963);

    @Test
    void findAllReturnsBooks() throws Exception {
        when(bookService.findAll()).thenReturn(List.of(book));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Rayuela"));
    }

    @Test
    void findByIdReturnsBook() throws Exception {
        when(bookService.findById(1L)).thenReturn(book);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Julio Cortázar"));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(bookService.findById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturns201() throws Exception {
        when(bookService.create(any(Book.class))).thenReturn(book);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Rayuela\",\"author\":\"Julio Cortázar\",\"isbn\":\"978-84-376-0494-7\",\"publishedYear\":1963}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturnsUpdatedBook() throws Exception {
        Book updated = new Book(1L, "Rayuela (edición nueva)", "Julio Cortázar", "978-84-376-0494-7", 2019);
        when(bookService.update(eq(1L), any(Book.class))).thenReturn(updated);

        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Rayuela (edición nueva)\",\"publishedYear\":2019}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedYear").value(2019));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());

        verify(bookService).delete(1L);
    }
}
