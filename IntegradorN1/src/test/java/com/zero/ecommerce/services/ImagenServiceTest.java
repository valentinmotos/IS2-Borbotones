package com.zero.ecommerce.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.zero.ecommerce.entities.Imagen;
import com.zero.ecommerce.entities.enums.TipoImagen;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.ImagenRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:?foreign_keys=on",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true" })
@AutoConfigureMockMvc
class ImagenServiceTest {

    @Autowired
    private ImagenService imagenService;

    @Autowired
    private ImagenRepository imagenRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void crearImagenValidaGuardaMetadatos() throws Exception {
        byte[] png = Files.readAllBytes(Path.of("src/test/resources/test-image.png"));
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "producto.png", "image/png", png);

        Imagen imagen = imagenService.crearImagen(archivo, TipoImagen.PRODUCTO);

        assertNotNull(imagen.getId());
        assertEquals("producto.png", imagen.getNombre());
        assertEquals("image/png", imagen.getMime());
        assertEquals(TipoImagen.PRODUCTO, imagen.getTipoImagen());
        assertEquals(png.length, imagen.getContenido().length);
    }

    @Test
    void crearImagenConArchivoVacioLanzaError() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "vacio.png", "image/png", new byte[0]);

        assertThrows(ErrorServiceException.class,
                () -> imagenService.crearImagen(archivo, TipoImagen.PRODUCTO));
    }

    @Test
    void crearImagenConFormatoNoPermitidoLanzaError() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "archivo.exe", "application/x-msdownload", new byte[] {1, 2, 3});

        assertThrows(ErrorServiceException.class,
                () -> imagenService.crearImagen(archivo, TipoImagen.PRODUCTO));
    }

    @Test
    void getImagenRetornaContenidoYContentType() throws Exception {
        byte[] jpeg = Files.readAllBytes(Path.of("src/test/resources/test-image.jpg"));
        Imagen imagen = imagenService.crearImagen(
                new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", jpeg),
                TipoImagen.PERSONA);

        mockMvc.perform(get("/imagen/{id}", imagen.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE));
    }
}
