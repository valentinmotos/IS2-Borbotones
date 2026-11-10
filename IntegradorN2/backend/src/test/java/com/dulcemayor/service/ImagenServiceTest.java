package com.dulcemayor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.dulcemayor.dto.ImagenCargaDTO;
import com.dulcemayor.dto.ImagenDTO;
import com.dulcemayor.dto.mapper.ImagenMapperImpl;
import com.dulcemayor.entity.Imagen;
import com.dulcemayor.entity.enums.TipoImagen;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.ImagenRepository;

/** E1-05: tipos permitidos por firma, tamaño máximo y archivo vacío. */
@ExtendWith(MockitoExtension.class)
class ImagenServiceTest {

    static final byte[] PNG = { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0 };
    static final byte[] JPG = { (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 0, 0 };
    static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    static final byte[] PDF = "%PDF-1.7 contenido".getBytes(StandardCharsets.ISO_8859_1);
    private static final int DOS_MEGAS = 2 * 1024 * 1024;

    @Mock
    private ImagenRepository repository;

    private ImagenService service;

    @BeforeEach
    void setUp() {
        // Mapper real que genera MapStruct
        service = new ImagenService(repository, new ImagenMapperImpl());
    }

    private void guardarDevuelveConId() {
        when(repository.save(any(Imagen.class))).thenAnswer(invocacion -> {
            Imagen imagen = invocacion.getArgument(0);
            imagen.setId("id-1");
            return imagen;
        });
    }

    private static ImagenCargaDTO carga(String nombre, byte[] contenido, TipoImagen tipo) {
        return new ImagenCargaDTO(new MockMultipartFile("archivo", nombre, "image/png", contenido), tipo);
    }

    @Test
    void aceptaPngJpgYWebpDetectandoElTipoPorElContenido() {
        guardarDevuelveConId();

        assertThat(service.crear(carga("a.png", PNG, TipoImagen.PRODUCTO)).mime()).isEqualTo("image/png");
        assertThat(service.crear(carga("b.jpg", JPG, TipoImagen.SUCURSAL)).mime()).isEqualTo("image/jpeg");
        assertThat(service.crear(carga("c.webp", WEBP, TipoImagen.PERFIL)).mime()).isEqualTo("image/webp");
    }

    @Test
    void subirDevuelveIdYUrlConElNombreLimpio() {
        guardarDevuelveConId();

        ImagenDTO imagen = service.crear(carga("C:\\fotos\\alfajor.png", PNG, TipoImagen.PRODUCTO));

        assertThat(imagen.id()).isEqualTo("id-1");
        assertThat(imagen.url()).isEqualTo("/api/imagenes/id-1");
        assertThat(imagen.nombre()).isEqualTo("alfajor.png");
    }

    @Test
    void unArchivoRenombradoAPngNoPasa() {
        assertThatThrownBy(() -> service.crear(carga("foto.png", PDF, TipoImagen.PRODUCTO)))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("El archivo debe ser JPG, PNG o WEBP.");
        verify(repository, never()).save(any());
    }

    @Test
    void masDeDosMegasSeRechaza() {
        byte[] grande = Arrays.copyOf(PNG, DOS_MEGAS + 1);

        assertThatThrownBy(() -> service.crear(carga("grande.png", grande, TipoImagen.PRODUCTO)))
                .hasMessage("La imagen no puede superar los 2 MB.");
        assertThatThrownBy(() -> service.crearImagen("grande.png", grande, TipoImagen.PRODUCTO))
                .isInstanceOf(ErrorServiceException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void archivoVacioOSinTipoSeRechaza() {
        assertThatThrownBy(() -> service.crear(carga("x.png", new byte[0], TipoImagen.PRODUCTO)))
                .hasMessage("Seleccioná una imagen para subir.");
        assertThatThrownBy(() -> service.crear(carga("a.png", PNG, null)))
                .hasMessage("El tipo de imagen es obligatorio.");
    }

    @Test
    void crearImagenDelSeederDetectaElTipo() {
        guardarDevuelveConId();

        assertThat(service.crearImagen("demo.webp", WEBP, TipoImagen.SUCURSAL).mime()).isEqualTo("image/webp");
    }
}
