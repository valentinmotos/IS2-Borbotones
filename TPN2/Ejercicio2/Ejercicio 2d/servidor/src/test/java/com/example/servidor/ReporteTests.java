package com.example.servidor;

import com.example.servidor.dao.*;
import com.example.servidor.model.*;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReporteTests {
    static final Path TEMP = crearDirectorio();
    @Autowired MockMvc mvc;
    @Autowired PrestamoDAO prestamos;
    @Autowired PersonaDAO personas;
    @Autowired LibroDAO libros;

    static Path crearDirectorio() {
        try { return Files.createTempDirectory("reportes-biblioteca-"); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMP.resolve("test.sqlite"));
    }

    @BeforeEach
    void limpiar() {
        prestamos.deleteAll();
        libros.deleteAll();
        personas.deleteAll();
    }

    Persona persona(String nombre) {
        Persona persona = new Persona();
        persona.setNombre(nombre);
        persona.setApellido("Pérez");
        persona.setDni(12345678);
        persona.setEmail(nombre + "@ejemplo.com");
        return personas.save(persona);
    }

    Libro libro(String titulo) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAutor("Autor de prueba");
        libro.setFecha(2020);
        libro.setGenero("Novela");
        libro.setPaginas(150);
        return libros.save(libro);
    }

    void prestamo(Persona persona, Libro libro, Boolean devuelto) {
        Prestamo prestamo = new Prestamo();
        prestamo.setPersona(persona);
        prestamo.setLibro(libro);
        prestamo.setDevuelto(devuelto);
        prestamo.setFechaPrestamo(LocalDate.of(2020, 1, 1));
        prestamo.setFechaDevolucion(LocalDate.of(2020, 2, 1));
        prestamos.save(prestamo);
    }

    byte[] pdf() throws Exception {
        return mvc.perform(get("/api/reportes/personas-alquileres.pdf"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"personas-alquileres.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();
    }

    byte[] excel() throws Exception {
        return mvc.perform(get("/api/reportes/libros-disponibles.xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"libros-disponibles.xlsx\""))
                .andReturn().getResponse().getContentAsByteArray();
    }

    String textoPdf(byte[] datos) throws Exception {
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(datos)))) {
            StringBuilder texto = new StringBuilder();
            for (int i = 1; i <= pdf.getNumberOfPages(); i++) texto.append(PdfTextExtractor.getTextFromPage(pdf.getPage(i)));
            return texto.toString();
        }
    }

    @Test
    void pdfIncluyeHistorialSinDuplicadosNiPersonasSinAlquiler() throws Exception {
        Persona ana = persona("Ana");
        Persona bruno = persona("Bruno");
        persona("SinAlquiler");
        prestamo(ana, libro("Primero"), false);
        prestamo(ana, libro("Segundo"), true);
        prestamo(bruno, libro("Tercero"), true);
        String texto = textoPdf(pdf());
        assertThat(texto).contains("Total de personas: 2", "Ana", "Bruno", "Pérez");
        assertThat(texto).doesNotContain("SinAlquiler");
        assertThat(texto.split("Ana@ejemplo.com", -1)).hasSize(2);
    }

    @Test
    void excelExcluyePendientesInclusoVencidosYAdmiteDevueltos() throws Exception {
        Persona ana = persona("Ana");
        libro("Disponible");
        Libro devuelto = libro("Devuelto");
        prestamo(ana, devuelto, true);
        prestamo(ana, libro("Ocupado"), false);
        prestamo(ana, libro("Estado nulo"), null);
        Libro mixto = libro("Mixto");
        prestamo(ana, mixto, true);
        prestamo(ana, mixto, false);
        try (XSSFWorkbook excel = new XSSFWorkbook(new ByteArrayInputStream(excel()))) {
            var hoja = excel.getSheet("Libros disponibles");
            assertThat(hoja.getLastRowNum()).isEqualTo(2);
            assertThat(hoja.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Devuelto");
            assertThat(hoja.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Disponible");
            assertThat(hoja.getRow(1).getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(hoja.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(150);
        }
    }

    @Test
    void reportesVaciosSonArchivosValidos() throws Exception {
        assertThat(textoPdf(pdf())).contains("No hay personas con alquileres registrados.");
        try (XSSFWorkbook excel = new XSSFWorkbook(new ByteArrayInputStream(excel()))) {
            assertThat(excel.getSheetAt(0).getLastRowNum()).isZero();
            assertThat(excel.getSheetAt(0).getRow(0).getCell(1).getStringCellValue()).isEqualTo("Título");
        }
    }

    @Test
    void pdfConMuchasPersonasContinuaEnOtrasPaginas() throws Exception {
        Libro libro = libro("Libro compartido");
        for (int i = 0; i < 70; i++) prestamo(persona(String.format("Persona%02d", i)), libro, true);
        byte[] datos = pdf();
        try (PdfDocument documento = new PdfDocument(new PdfReader(new ByteArrayInputStream(datos)))) {
            assertThat(documento.getNumberOfPages()).isGreaterThan(1);
        }
        assertThat(textoPdf(datos)).contains("Persona00", "Persona69", "Total de personas: 70");
    }
}
