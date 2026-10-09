package com.example.mascotas.cliente;

import com.example.mascotas.dto.Dto.*;
import com.example.mascotas.enumeracion.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Activa --demo=true para ejecutar el flujo contra la API mediante RestTemplate. */
@Component
@ConditionalOnProperty(name = "demo", havingValue = "true")
public class DemoRestTemplate implements ApplicationRunner {
    private final TinderRestClient client;
    public DemoRestTemplate(TinderRestClient client) { this.client = client; }
    @Override
    public void run(ApplicationArguments args) {
        String suffix = java.util.UUID.randomUUID().toString();
        String ana = "ana." + suffix + "@demo.test";
        String juan = "juan." + suffix + "@demo.test";
        String clave = "mascotas123";
        ZonaDto zona = client.crearZona(new ZonaRequest("Demo " + suffix, "Zona de prueba"));
        client.registrar(new UsuarioRequest("Ana", "Perez", ana, clave, clave, zona.id()));
        client.registrar(new UsuarioRequest("Juan", "Lopez", juan, clave, clave, zona.id()));
        client.login(new LoginRequest(ana, clave));
        MascotaDto luna = client.crearMascota(ana, clave, new MascotaRequest("Luna", Sexo.HEMBRA, Tipo.PERRO));
        MascotaDto toby = client.crearMascota(juan, clave, new MascotaRequest("Toby", Sexo.MACHO, Tipo.PERRO));
        System.out.println("RestTemplate - candidatos: " + client.candidatos(ana, clave, luna.id()));
        VotoDto voto = client.votar(ana, clave, new VotoRequest(luna.id(), toby.id()));
        client.responder(juan, clave, voto.id());
        System.out.println("RestTemplate - matches: " + client.matches(ana, clave));
        System.out.println("DEMO RESTTEMPLATE OK: registro, login, mascotas, candidatos, voto y match");
    }
}
