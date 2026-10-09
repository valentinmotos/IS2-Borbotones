package com.example.servidor.service;

import com.example.servidor.dao.ProveedorDAO;
import com.example.servidor.model.Proveedor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MigracionService {
    private final ProveedorDAO proveedorDAO;

    public MigracionService(ProveedorDAO proveedorDAO) {
        this.proveedorDAO = proveedorDAO;
    }

    @Transactional
    public int importar(byte[] contenido) {
        List<Proveedor> proveedores = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(contenido), StandardCharsets.UTF_8))) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.isBlank()) continue;

                String[] campos = linea.split(";", -1);
                if (campos.length == 6 && campos[5].isBlank()) {
                    campos = java.util.Arrays.copyOf(campos, 5);
                }
                if (campos.length != 5) {
                    throw error("La línea " + numeroLinea + " debe tener 5 campos separados por punto y coma.");
                }

                String nombre = campos[0].trim();
                String apellido = campos[1].trim();
                String dni = campos[2].trim();
                String calle = campos[3].trim();
                String numeroTexto = campos[4].trim();
                if (nombre.isEmpty() || apellido.isEmpty() || dni.isEmpty() || calle.isEmpty() || numeroTexto.isEmpty()) {
                    throw error("La línea " + numeroLinea + " contiene campos vacíos.");
                }

                Proveedor proveedor = new Proveedor();
                proveedor.setNombre(nombre);
                proveedor.setApellido(apellido);
                proveedor.setDni(dni);
                proveedor.setCalle(calle);
                try {
                    proveedor.setNumero(Integer.valueOf(numeroTexto));
                } catch (NumberFormatException ex) {
                    throw error("El número de domicilio de la línea " + numeroLinea + " debe ser numérico.");
                }
                proveedores.add(proveedor);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo de migración.", ex);
        }

        if (proveedores.isEmpty()) {
            throw error("El archivo no contiene proveedores para importar.");
        }
        proveedorDAO.saveAll(proveedores);
        return proveedores.size();
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
