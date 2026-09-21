package com.borbotones.videojuegos;

import com.borbotones.videojuegos.services.ImagenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagenServiceTests {
    @TempDir
    Path directorioTemporal;

    @Test
    void ignoraElNombreOriginalYGeneraUnNombreSeguro() throws Exception {
        ByteArrayOutputStream contenido = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", contenido);
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "../../archivo-peligroso.png", "image/png", contenido.toByteArray());

        String nombre = new ImagenService(directorioTemporal.toString()).guardar(archivo, true);

        assertThat(nombre).matches("[0-9a-f-]{36}\\.png");
        assertThat(Files.isRegularFile(directorioTemporal.resolve(nombre))).isTrue();
    }

    @Test
    void rechazaContenidoQueNoEsUnaImagenReal() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "falsa.png", "image/png", "no es una imagen".getBytes());

        assertThatThrownBy(() -> new ImagenService(directorioTemporal.toString()).guardar(archivo, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPG o PNG");
    }
}
