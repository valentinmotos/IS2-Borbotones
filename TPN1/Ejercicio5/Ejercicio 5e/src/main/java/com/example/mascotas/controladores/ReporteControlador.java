package com.example.mascotas.controladores;

import com.example.mascotas.dto.ReporteMascotaDTO;
import com.example.mascotas.servicios.ReporteServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/reporte")
public class ReporteControlador {

    @Autowired
    private ReporteServicio reporteServicio;

    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargar(HttpSession session) {
        if (session.getAttribute("usuariosession") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ReporteMascotaDTO> datos = reporteServicio.obtenerInformacion();
        byte[] contenido = crearContenido(datos).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-mascotas.txt\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .contentLength(contenido.length)
                .body(contenido);
    }

    private String crearContenido(List<ReporteMascotaDTO> datos) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("Nombre de usuario\tApellido del usuario\tNombre de la mascota\tCantidad de votos")
                .append(System.lineSeparator());

        for (ReporteMascotaDTO dato : datos) {
            contenido.append(limpiar(dato.getNombreUsuario())).append('\t')
                    .append(limpiar(dato.getApellidoUsuario())).append('\t')
                    .append(limpiar(dato.getNombreMascota())).append('\t')
                    .append(dato.getCantidadVotos())
                    .append(System.lineSeparator());
        }

        return contenido.toString();
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }
}
