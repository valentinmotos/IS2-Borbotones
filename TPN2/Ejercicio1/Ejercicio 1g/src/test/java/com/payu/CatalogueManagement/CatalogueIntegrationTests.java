package com.payu.CatalogueManagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.payu.CatalogueManagement.entity.Book;
import com.payu.CatalogueManagement.entity.BookType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogueIntegrationTests {
    @Autowired TestRestTemplate rest;

    @Test
    void servirFrontendYRecursos() {
        var pagina = rest.getForEntity("/", String.class);
        assertThat(pagina.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pagina.getBody()).contains("Mi biblioteca", "Agregar libro", "/app.js", "/styles.css");
        assertThat(rest.getForEntity("/app.js", String.class).getBody()).contains("/getAllBooks", "/addBook", "/updateBook/", "/deleteBook/");
        assertThat(rest.getForEntity("/styles.css", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void crearConsultarEditarYEliminarLibro() {
        Book book = new Book(null, "Prueba integrada", "9780000000001", LocalDate.of(2020, 5, 15), 1200.0, BookType.HARDCOVER);
        var creado = rest.postForEntity("/api/books/addBook", book, Book.class);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long id = creado.getBody().getId();
        assertThat(id).isNotNull();
        assertThat(rest.getForObject("/api/books/" + id, Book.class).getName()).isEqualTo("Prueba integrada");
        assertThat(rest.getForObject("/api/books/getAllBooks", Book[].class))
                .anyMatch(item -> item.getId().equals(id));
        book.setName("Prueba editada");
        book.setBookType(BookType.EBOOK);
        var editado = rest.exchange("/api/books/updateBook/" + id, HttpMethod.PUT, new HttpEntity<>(book), Book.class);
        assertThat(editado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(editado.getBody().getName()).isEqualTo("Prueba editada");
        assertThat(editado.getBody().getBookType()).isEqualTo(BookType.EBOOK);
        var eliminado = rest.exchange("/api/books/deleteBook/" + id, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
        assertThat(eliminado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rest.getForEntity("/api/books/" + id, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rechazarDatosInvalidosYEdicionInexistente() {
        var invalido = rest.postForEntity("/api/books/addBook", Map.of("name", " ", "price", -10), String.class);
        assertThat(invalido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Book book = new Book(null, "Valido", "123", LocalDate.now(), 100.0, BookType.EBOOK);
        assertThat(rest.exchange("/api/books/updateBook/999999", HttpMethod.PUT,
                new HttpEntity<>(book), String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void graphqlConsultaYActualizacionParcial() {
        Book book = rest.postForObject("/api/books/addBook",
                new Book(null, "GraphQL", "987654", LocalDate.of(2021, 1, 1), 100.0, BookType.SOFTCOVER), Book.class);
        long id = book.getId();
        JsonNode consulta = rest.postForObject("/graphql", Map.of("query",
                "{ getBookById(id: \"" + id + "\") { name isbnNumber } }"), JsonNode.class);
        assertThat(consulta.has("errors")).isFalse();
        assertThat(consulta.at("/data/getBookById/name").asText()).isEqualTo("GraphQL");
        JsonNode cambio = rest.postForObject("/graphql", Map.of("query",
                "mutation { updateBook(id: \"" + id + "\", name: \"Nuevo titulo\") { name isbnNumber price } }"), JsonNode.class);
        assertThat(cambio.has("errors")).isFalse();
        assertThat(cambio.at("/data/updateBook/name").asText()).isEqualTo("Nuevo titulo");
        assertThat(cambio.at("/data/updateBook/isbnNumber").asText()).isEqualTo("987654");
        rest.delete("/api/books/deleteBook/" + id);
    }
}
