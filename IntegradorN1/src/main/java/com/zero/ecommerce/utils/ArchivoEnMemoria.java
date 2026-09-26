package com.zero.ecommerce.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.springframework.web.multipart.MultipartFile;

/**
 * MultipartFile armado con bytes que ya están en memoria, por ejemplo una imagen leída del
 * classpath. Permite cargar archivos por ImagenService sin un formulario, con sus mismas validaciones.
 * Lo usa el DataSeeder para las imágenes de seed/img.
 */
public class ArchivoEnMemoria implements MultipartFile {

    private final String nombreArchivo;
    private final String tipo;
    private final byte[] contenido;

    public ArchivoEnMemoria(String nombreArchivo, String tipo, byte[] contenido) {
        this.nombreArchivo = nombreArchivo;
        this.tipo = tipo;
        this.contenido = contenido == null ? new byte[0] : contenido;
    }

    @Override
    public String getName() {
        return "archivo";
    }

    @Override
    public String getOriginalFilename() {
        return nombreArchivo;
    }

    @Override
    public String getContentType() {
        return tipo;
    }

    @Override
    public boolean isEmpty() {
        return contenido.length == 0;
    }

    @Override
    public long getSize() {
        return contenido.length;
    }

    @Override
    public byte[] getBytes() {
        return contenido.clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(contenido);
    }

    @Override
    public void transferTo(File destino) throws IOException {
        Files.write(destino.toPath(), contenido);
    }
}
