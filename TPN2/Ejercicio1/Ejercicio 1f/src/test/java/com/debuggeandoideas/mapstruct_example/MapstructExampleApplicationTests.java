package com.debuggeandoideas.mapstruct_example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import com.debuggeandoideas.mapstruct_example.dao.CountryDao;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MapstructExampleApplicationTests {

    @Autowired
    private TestRestTemplate rest;

	@Test
	void listarPaises() {
		var respuesta = rest.getForEntity("/country", JsonNode.class);
		assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(respuesta.getBody().isArray()).isTrue();
		assertThat(respuesta.getBody().size()).isEqualTo(CountryDao.db.size());
		assertThat(respuesta.getBody().get(0).get("id").asText()).isNotBlank();
	}

    @Test
    void consultarPaisMapeaCamposAnidadosYOcultaDatosInternos() {
        var pais = CountryDao.db.values().stream()
                .filter(country -> country.getName().equals("Brazil")).findFirst().orElseThrow();
        var respuesta = rest.getForEntity("/country/" + pais.getId(), JsonNode.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode dto = respuesta.getBody();
        assertThat(dto.get("name").asText()).isEqualTo(pais.getName());
        assertThat(dto.get("continent").asText()).isEqualTo(pais.getLocation().getContinent());
        assertThat(dto.get("languages").get(0).get("isOfficialLanguage").asBoolean()).isTrue();
        assertThat(dto.get("languages").get(0).get("speakersTotal").asInt()).isEqualTo(211000000);
        assertThat(dto.get("ecosystems").get(0).get("climateType").asText()).isEqualTo("Tropical");
        assertThat(dto.has("president")).isFalse();
        assertThat(dto.has("location")).isFalse();
        assertThat(dto.get("ecosystems").get(0).has("predominantSpecies")).isFalse();
    }

    @Test
    void paisInexistenteDevuelve404() {
        assertThat(rest.getForEntity("/country/" + new UUID(0, 0), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void identificadorInvalidoDevuelve400() {
        assertThat(rest.getForEntity("/country/no-es-uuid", String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
