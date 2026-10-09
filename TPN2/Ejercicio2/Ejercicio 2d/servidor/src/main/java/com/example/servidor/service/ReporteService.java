package com.example.servidor.service;

import com.example.servidor.dao.LibroDAO;
import com.example.servidor.dao.PrestamoDAO;
import com.example.servidor.model.Libro;
import com.example.servidor.model.Persona;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReporteService {
    private final PrestamoDAO prestamoDAO;
    private final LibroDAO libroDAO;

    public ReporteService(PrestamoDAO prestamoDAO, LibroDAO libroDAO) {
        this.prestamoDAO = prestamoDAO;
        this.libroDAO = libroDAO;
    }

    public byte[] personasConAlquileresPdf() {
        List<Persona> personas = prestamoDAO.findPersonasConAlquileres();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (Document documento = new Document(new PdfDocument(new PdfWriter(salida)), PageSize.A4.rotate())) {
            documento.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA)).setFontSize(10);
            documento.add(new Paragraph("Personas que realizaron alquiler de libros").setFontSize(18));
            documento.add(new Paragraph("Incluye préstamos activos y devueltos. Cada persona se muestra una sola vez."));
            documento.add(new Paragraph("Total de personas: " + personas.size()));
            if (personas.isEmpty()) {
                documento.add(new Paragraph("No hay personas con alquileres registrados."));
            } else {
                Table tabla = new Table(UnitValue.createPercentArray(new float[]{7, 22, 22, 15, 34})).useAllAvailableWidth();
                for (String titulo : List.of("ID", "Apellido", "Nombre", "DNI", "Email")) {
                    tabla.addHeaderCell(new Cell().add(new Paragraph(titulo))
                            .setBackgroundColor(new DeviceRgb(225, 234, 241)));
                }
                for (Persona persona : personas) {
                    for (Object dato : new Object[]{persona.getId(), persona.getApellido(), persona.getNombre(), persona.getDni(), persona.getEmail()}) {
                        tabla.addCell(new Cell().add(new Paragraph(texto(dato))));
                    }
                }
                documento.add(tabla);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el PDF", ex);
        }
        return salida.toByteArray();
    }

    public byte[] librosDisponiblesExcel() {
        List<Libro> libros = libroDAO.findLibrosDisponibles();
        try (XSSFWorkbook libroExcel = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libroExcel.createSheet("Libros disponibles");
            String[] columnas = {"ID", "Título", "Año", "Género", "Páginas", "Autor"};
            CellStyle encabezado = libroExcel.createCellStyle();
            Font fuente = libroExcel.createFont();
            fuente.setBold(true);
            fuente.setColor(IndexedColors.WHITE.getIndex());
            encabezado.setFont(fuente);
            encabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row fila = hoja.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                org.apache.poi.ss.usermodel.Cell celda = fila.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(encabezado);
            }
            int numeroFila = 1;
            for (Libro libro : libros) {
                fila = hoja.createRow(numeroFila++);
                numero(fila, 0, libro.getId());
                fila.createCell(1).setCellValue(texto(libro.getTitulo()));
                numero(fila, 2, libro.getFecha());
                fila.createCell(3).setCellValue(texto(libro.getGenero()));
                numero(fila, 4, libro.getPaginas());
                fila.createCell(5).setCellValue(texto(libro.getAutor()));
            }
            hoja.createFreezePane(0, 1);
            hoja.setAutoFilter(new CellRangeAddress(0, Math.max(0, numeroFila - 1), 0, columnas.length - 1));
            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
                hoja.setColumnWidth(i, Math.min(60 * 256, Math.max(12 * 256, hoja.getColumnWidth(i) + 512)));
            }
            libroExcel.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el Excel", ex);
        }
    }

    private static String texto(Object dato) {
        return dato == null ? "" : dato.toString();
    }

    private static void numero(Row fila, int columna, Number valor) {
        if (valor != null) fila.createCell(columna).setCellValue(valor.doubleValue());
        else fila.createCell(columna);
    }
}
