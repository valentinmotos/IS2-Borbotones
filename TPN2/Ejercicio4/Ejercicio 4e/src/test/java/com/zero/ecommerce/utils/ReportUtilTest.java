package com.zero.ecommerce.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.zero.ecommerce.dto.RenglonFacturaDTO;

import net.sf.jasperreports.engine.JRException;

class ReportUtilTest {

    @Test
    void generaLaFacturaEnPdf() throws Exception {
        Map<String, Object> parametros = new HashMap<>(Map.of(
                "EMPRESA_NOMBRE", "Zero Indumentaria Deportiva S.A.",
                "NUMERO", "00000001",
                "CLIENTE_NOMBRE", "Lucía Gómez",
                "TOTAL", 30000.0));
        List<RenglonFacturaDTO> renglones = List.of(
                new RenglonFacturaDTO("REM-DRY-H-M", "Remera Zero Dry Fit Hombre", "M", 2, 10000.0, 20000.0));

        byte[] pdf = ReportUtil.generarPdf("factura", parametros, renglones);

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void fallaSiElReporteNoExiste() {
        assertThatThrownBy(() -> ReportUtil.generarPdf("no-existe", Map.of(), List.of()))
                .isInstanceOf(JRException.class);
    }
}
