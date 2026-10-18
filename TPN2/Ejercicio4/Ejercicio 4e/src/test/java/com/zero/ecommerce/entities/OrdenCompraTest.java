package com.zero.ecommerce.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.zero.ecommerce.dto.PasoSeguimientoDTO;
import com.zero.ecommerce.entities.enums.EstadoOrdenCompra;
import com.zero.ecommerce.exception.ErrorServiceException;

/**
 * Tests de las transiciones de estado de la orden (E4-01), válidas e inválidas.
 */
class OrdenCompraTest {

    private OrdenCompra orden;

    @BeforeEach
    void setUp() {
        Producto producto = new Producto();
        producto.setId("p1");
        producto.setNombre("Remera Zero");

        orden = new OrdenCompra();
        orden.crearDetalle(producto, 2, 1000);
        orden.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
    }

    private OrdenCompra ordenEn(EstadoOrdenCompra estado) {
        orden.setEstadoOrdenCompra(estado);
        return orden;
    }

    // --- confirmar() ---------------------------------------------------------------------------------------------

    @Test
    void confirmarPasaDePendienteCompletarAPendientePago() throws Exception {
        orden.confirmar();

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_PAGO);
    }

    @Test
    void confirmarCarritoVacioLanzaExcepcion() {
        OrdenCompra vacia = new OrdenCompra();
        vacia.setEstadoOrdenCompra(EstadoOrdenCompra.PENDIENTE_COMPLETAR);

        assertThatThrownBy(vacia::confirmar)
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede confirmar un carrito vacío.");
        assertThat(vacia.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_COMPLETAR);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_PAGO", "PENDIENTE_ENVIO", "PENDIENTE_ENTREGA",
            "ENTREGADO" })
    void confirmarOrdenYaConfirmadaLanzaExcepcion(EstadoOrdenCompra estado) {
        assertThatThrownBy(ordenEn(estado)::confirmar)
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La orden ya fue confirmada.");
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(estado);
    }

    // --- registrarPago() -----------------------------------------------------------------------------------------

    @Test
    void registrarPagoPasaDePendientePagoAPendienteEnvio() throws Exception {
        ordenEn(EstadoOrdenCompra.PENDIENTE_PAGO).registrarPago();

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
    }

    @Test
    void registrarPagoSinConfirmarLanzaExcepcion() {
        assertThatThrownBy(ordenEn(EstadoOrdenCompra.PENDIENTE_COMPLETAR)::registrarPago)
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede pagar una orden sin confirmar.");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_ENVIO", "PENDIENTE_ENTREGA", "ENTREGADO" })
    void registrarPagoOrdenYaPagadaLanzaExcepcion(EstadoOrdenCompra estado) {
        assertThatThrownBy(ordenEn(estado)::registrarPago)
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La orden ya fue pagada.");
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(estado);
    }

    // --- marcarEnviado() -----------------------------------------------------------------------------------------

    @Test
    void marcarEnviadoPasaDePendienteEnvioAPendienteEntrega() throws Exception {
        ordenEn(EstadoOrdenCompra.PENDIENTE_ENVIO).marcarEnviado();

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENTREGA);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_COMPLETAR", "PENDIENTE_PAGO" })
    void marcarEnviadoSinPagarLanzaExcepcion(EstadoOrdenCompra estado) {
        assertThatThrownBy(ordenEn(estado)::marcarEnviado)
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede enviar una orden sin pagar.");
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(estado);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_ENTREGA", "ENTREGADO" })
    void marcarEnviadoOrdenYaEnviadaLanzaExcepcion(EstadoOrdenCompra estado) {
        assertThatThrownBy(ordenEn(estado)::marcarEnviado)
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La orden ya fue enviada.");
    }

    // --- marcarEntregado() ---------------------------------------------------------------------------------------

    @Test
    void marcarEntregadoPasaDePendienteEntregaAEntregado() throws Exception {
        ordenEn(EstadoOrdenCompra.PENDIENTE_ENTREGA).marcarEntregado();

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ENTREGADO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_COMPLETAR", "PENDIENTE_PAGO",
            "PENDIENTE_ENVIO" })
    void marcarEntregadoSinEnviarLanzaExcepcion(EstadoOrdenCompra estado) {
        assertThatThrownBy(ordenEn(estado)::marcarEntregado)
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede entregar una orden que no fue enviada.");
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(estado);
    }

    @Test
    void marcarEntregadoOrdenYaEntregadaLanzaExcepcion() {
        assertThatThrownBy(ordenEn(EstadoOrdenCompra.ENTREGADO)::marcarEntregado)
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La orden ya fue entregada.");
    }

    // --- Orden anulada: no admite ninguna transición -------------------------------------------------------------

    @Test
    void ordenAnuladaNoAdmiteCambiosDeEstado() {
        ordenEn(EstadoOrdenCompra.ANULADA);
        String mensaje = "La orden está anulada y no admite cambios de estado.";

        assertThatThrownBy(orden::confirmar).hasMessage(mensaje);
        assertThatThrownBy(orden::registrarPago).hasMessage(mensaje);
        assertThatThrownBy(orden::marcarEnviado).hasMessage(mensaje);
        assertThatThrownBy(orden::marcarEntregado).hasMessage(mensaje);
        assertThatThrownBy(() -> orden.anular(false)).hasMessage(mensaje);
        assertThatThrownBy(() -> orden.anular(true)).hasMessage(mensaje);
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ANULADA);
    }

    // --- puedeAnularse(esAdmin) ----------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_COMPLETAR", "PENDIENTE_PAGO" })
    void clienteYAdminPuedenAnularAntesDelPago(EstadoOrdenCompra estado) {
        ordenEn(estado);

        assertThat(orden.puedeAnularse(false)).isTrue();
        assertThat(orden.puedeAnularse(true)).isTrue();
    }

    @Test
    void soloAdminPuedeAnularEnPendienteEnvio() {
        ordenEn(EstadoOrdenCompra.PENDIENTE_ENVIO);

        assertThat(orden.puedeAnularse(false)).isFalse();
        assertThat(orden.puedeAnularse(true)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_ENTREGA", "ENTREGADO", "ANULADA",
            "SIN_DEFINIR" })
    void nadiePuedeAnularDesdePendienteEntregaEnAdelante(EstadoOrdenCompra estado) {
        ordenEn(estado);

        assertThat(orden.puedeAnularse(false)).isFalse();
        assertThat(orden.puedeAnularse(true)).isFalse();
    }

    @Test
    void ordenSinEstadoNoPuedeAnularse() {
        orden.setEstadoOrdenCompra(null);

        assertThat(orden.puedeAnularse(true)).isFalse();
    }

    // --- anular(esAdmin) -----------------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_COMPLETAR", "PENDIENTE_PAGO" })
    void clienteAnulaAntesDelPago(EstadoOrdenCompra estado) throws Exception {
        ordenEn(estado).anular(false);

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ANULADA);
    }

    @Test
    void adminAnulaEnPendienteEnvio() throws Exception {
        ordenEn(EstadoOrdenCompra.PENDIENTE_ENVIO).anular(true);

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ANULADA);
    }

    @Test
    void clienteNoPuedeAnularOrdenPagada() {
        assertThatThrownBy(() -> ordenEn(EstadoOrdenCompra.PENDIENTE_ENVIO).anular(false))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("La orden ya fue pagada. Para anularla comunicate con la tienda.");
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.PENDIENTE_ENVIO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_ENTREGA", "ENTREGADO" })
    void nadieAnulaOrdenEnviada(EstadoOrdenCompra estado) {
        assertThatThrownBy(() -> ordenEn(estado).anular(true))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede anular una orden que ya fue enviada.");
        assertThatThrownBy(() -> orden.anular(false))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessage("No se puede anular una orden que ya fue enviada.");
        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(estado);
    }

    // --- Flujo completo ------------------------------------------------------------------------------------------

    @Test
    void flujoCompletoDelCarritoAlEntregado() throws Exception {
        orden.confirmar();
        orden.registrarPago();
        orden.marcarEnviado();
        orden.marcarEntregado();

        assertThat(orden.getEstadoOrdenCompra()).isEqualTo(EstadoOrdenCompra.ENTREGADO);
    }

    // --- pasosSeguimiento() --------------------------------------------------------------------------------------

    @Test
    void pasosSeguimientoTieneLosCincoPasosEnOrden() {
        List<PasoSeguimientoDTO> pasos = ordenEn(EstadoOrdenCompra.PENDIENTE_PAGO).pasosSeguimiento();

        assertThat(pasos).extracting(PasoSeguimientoDTO::nombre).containsExactly(
                "Pendiente de pago", "Pago realizado", "Pendiente de envío", "Pendiente de entrega", "Entregado");
    }

    @Test
    void pasosSeguimientoEnPendientePago() {
        List<PasoSeguimientoDTO> pasos = ordenEn(EstadoOrdenCompra.PENDIENTE_PAGO).pasosSeguimiento();

        assertThat(pasos).extracting(PasoSeguimientoDTO::completo).containsExactly(false, false, false, false, false);
        assertThat(pasos).extracting(PasoSeguimientoDTO::actual).containsExactly(true, false, false, false, false);
    }

    @Test
    void pasosSeguimientoEnPendienteEnvioMarcaElPagoComoCompleto() {
        List<PasoSeguimientoDTO> pasos = ordenEn(EstadoOrdenCompra.PENDIENTE_ENVIO).pasosSeguimiento();

        assertThat(pasos).extracting(PasoSeguimientoDTO::completo).containsExactly(true, true, false, false, false);
        assertThat(pasos).extracting(PasoSeguimientoDTO::actual).containsExactly(false, false, true, false, false);
    }

    @Test
    void pasosSeguimientoEnPendienteEntrega() {
        List<PasoSeguimientoDTO> pasos = ordenEn(EstadoOrdenCompra.PENDIENTE_ENTREGA).pasosSeguimiento();

        assertThat(pasos).extracting(PasoSeguimientoDTO::completo).containsExactly(true, true, true, false, false);
        assertThat(pasos).extracting(PasoSeguimientoDTO::actual).containsExactly(false, false, false, true, false);
    }

    @Test
    void pasosSeguimientoEnEntregadoTieneTodoCompleto() {
        List<PasoSeguimientoDTO> pasos = ordenEn(EstadoOrdenCompra.ENTREGADO).pasosSeguimiento();

        assertThat(pasos).extracting(PasoSeguimientoDTO::completo).containsExactly(true, true, true, true, true);
        assertThat(pasos).extracting(PasoSeguimientoDTO::actual).containsExactly(false, false, false, false, true);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCompra.class, names = { "PENDIENTE_COMPLETAR", "ANULADA", "SIN_DEFINIR" })
    void pasosSeguimientoFueraDeLaLineaDeTiempoNoTieneNadaMarcado(EstadoOrdenCompra estado) {
        List<PasoSeguimientoDTO> pasos = ordenEn(estado).pasosSeguimiento();

        assertThat(pasos).hasSize(5);
        assertThat(pasos).noneMatch(PasoSeguimientoDTO::completo);
        assertThat(pasos).noneMatch(PasoSeguimientoDTO::actual);
    }
}
