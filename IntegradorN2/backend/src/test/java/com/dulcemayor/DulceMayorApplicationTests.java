package com.dulcemayor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * La aplicación completa levanta (contexto de Spring, JPA, seguridad y DataSeeder)
 * contra la base en memoria del perfil "test".
 */
@SpringBootTest
@ActiveProfiles("test")
class DulceMayorApplicationTests {

    @Test
    void elContextoLevanta() {
    }
}
