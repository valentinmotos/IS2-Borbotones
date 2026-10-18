package com.example.servidor.config;

import com.example.servidor.model.Libro;
import com.example.servidor.model.Localidad;
import com.example.servidor.model.EnvioAutomatico;
import com.example.servidor.service.EnvioAutomaticoService;
import com.example.servidor.service.LibroService;
import com.example.servidor.service.LocalidadService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final LocalidadService localidadService;
    private final LibroService libroService;
    private final EnvioAutomaticoService envioAutomaticoService;

    public DataSeeder(LocalidadService localidadService, LibroService libroService, EnvioAutomaticoService envioAutomaticoService) {
        this.localidadService = localidadService;
        this.libroService = libroService;
        this.envioAutomaticoService = envioAutomaticoService;
    }

    @Override
    public void run(String... args) {
        crearLocalidad("Mendoza");
        crearLocalidad("Godoy Cruz");
        crearLocalidad("Guaymallen");
        crearLocalidad("Las Heras");

        if (libroService.listar().isEmpty()) {
            crearLibro("El principito", 1943, "Fabula", 96, "Antoine de Saint-Exupery");
            crearLibro("Cien anos de soledad", 1967, "Realismo magico", 471, "Gabriel Garcia Marquez");
            crearLibro("Rayuela", 1963, "Novela", 736, "Julio Cortazar");
        }

        crearEnvioDevolucion();
        crearEnvioCumpleanos();
    }

    private void crearLocalidad(String denominacion) {
        if (!localidadService.existeDenominacion(denominacion)) {
            Localidad localidad = new Localidad();
            localidad.setDenominacion(denominacion);
            localidadService.guardar(localidad);
        }
    }

    private void crearLibro(String titulo, Integer fecha, String genero, Integer paginas, String autor) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setFecha(fecha);
        libro.setGenero(genero);
        libro.setPaginas(paginas);
        libro.setAutor(autor);
        libroService.guardar(libro);
    }

    private void crearEnvioDevolucion() {
        if (!envioAutomaticoService.existeTipo(EnvioAutomaticoService.TIPO_DEVOLUCION)) {
            EnvioAutomatico envio = new EnvioAutomatico();
            envio.setNombre("Recordatorio de devolucion");
            envio.setTipo(EnvioAutomaticoService.TIPO_DEVOLUCION);
            envio.setAsunto("Tu prestamo vence manana");
            envio.setCuerpoHtml("<h1>Hola {nombre}</h1><p>Te recordamos que el libro <strong>{libro}</strong> vence el {fechaDevolucion}.</p>");
            envio.setActivo(true);
            envioAutomaticoService.guardar(envio);
        }
    }

    private void crearEnvioCumpleanos() {
        if (!envioAutomaticoService.existeTipo(EnvioAutomaticoService.TIPO_CUMPLEANOS)) {
            EnvioAutomatico envio = new EnvioAutomatico();
            envio.setNombre("Saludo de cumpleanos");
            envio.setTipo(EnvioAutomaticoService.TIPO_CUMPLEANOS);
            envio.setAsunto("Feliz cumpleanos");
            envio.setCuerpoHtml("<h1>Feliz cumpleanos, {nombre}!</h1><p>Que tengas un gran dia.</p><p><a href=\"https://www.frm.utn.edu.ar/\" style=\"display:inline-block;padding:12px 18px;background:#2f80ed;color:#ffffff;text-decoration:none;border-radius:6px;\">Ir a la facultad</a></p>");
            envio.setActivo(true);
            envioAutomaticoService.guardar(envio);
        }
    }
}
