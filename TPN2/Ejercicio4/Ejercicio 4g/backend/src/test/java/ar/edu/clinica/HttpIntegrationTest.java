package ar.edu.clinica;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** HTTP real, sin transacción de test: verifica autenticación y commits entre peticiones. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:clinica-http;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop","clinica.demo=true","debug=false","logging.level.root=WARN"})
class HttpIntegrationTest {
    @Autowired TestRestTemplate rest;
    @Test void autenticaFueraDeTransaccionYConfirmaCrud() {
        assertEquals(401,rest.getForEntity("/api/usuarios",String.class).getStatusCode().value());
        var cliente=rest.withBasicAuth("demo","Consultorio123!");
        var usuarios=cliente.getForEntity("/api/usuarios",String.class);
        assertEquals(200,usuarios.getStatusCode().value()); assertFalse(usuarios.getBody().contains("clave"));
        var alta=cliente.postForEntity("/api/pacientes",Map.of("nombre","Prueba","apellido","HTTP","documento","HTTP-123"),Map.class);
        assertEquals(201,alta.getStatusCode().value());
        String ruta="/api/pacientes/"+alta.getBody().get("id");
        assertEquals(200,cliente.getForEntity(ruta,String.class).getStatusCode().value());
        assertEquals(204,cliente.exchange(ruta,HttpMethod.DELETE,HttpEntity.EMPTY,String.class).getStatusCode().value());
        assertEquals(404,cliente.getForEntity(ruta,String.class).getStatusCode().value());
        var historias=cliente.getForEntity("/api/historias",Map[].class).getBody();
        var expediente=cliente.getForEntity("/api/historias/"+historias[0].get("id"),Map.class);
        assertEquals(200,expediente.getStatusCode().value());
        assertEquals(1,expediente.getBody().get("totalAtenciones"));
    }
}
