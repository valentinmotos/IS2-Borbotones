package com.zero.ecommerce.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.dto.FilaTablaDTO;
import com.zero.ecommerce.entities.FormaDePago;
import com.zero.ecommerce.entities.enums.TipoPago;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.FormaDePagoRepository;

@Service
@Transactional(readOnly = true)
public class FormaDePagoService {

    private static final int LARGO_MAXIMO_OBSERVACION = 255;
    private final FormaDePagoRepository repository;

    public FormaDePagoService(FormaDePagoRepository repository) {
        this.repository = repository;
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void crearFormaDePago(TipoPago tipoPago, String observacion) throws ErrorServiceException {
        validar(tipoPago, observacion);
        FormaDePago formaDePago = new FormaDePago();
        formaDePago.setTipoPago(tipoPago);
        formaDePago.setObservacion(observacion.strip());
        repository.save(formaDePago);
    }

    public void validar(TipoPago tipoPago, String observacion) throws ErrorServiceException {
        validar(tipoPago, observacion, null);
    }

    private void validar(TipoPago tipoPago, String observacion, String idActual) throws ErrorServiceException {
        if (tipoPago == null) {
            throw new ErrorServiceException("El tipo de pago es obligatorio.");
        }
        if (observacion == null || observacion.isBlank()) {
            throw new ErrorServiceException("La observación de la forma de pago es obligatoria.");
        }
        if (observacion.strip().length() > LARGO_MAXIMO_OBSERVACION) {
            throw new ErrorServiceException("La observación no puede superar los "
                    + LARGO_MAXIMO_OBSERVACION + " caracteres.");
        }
        Optional<FormaDePago> duplicada = encontrarActiva(tipoPago, observacion);
        if (duplicada.isPresent() && !duplicada.get().getId().equals(idActual)) {
            throw new ErrorServiceException("Ya existe una forma de pago activa con ese tipo y observación.");
        }
    }

    // A diferencia de Nacionalidad, solo se comparan las activas: una forma de pago
    // desactivada no se puede reactivar, así que se permite volver a darla de alta.
    // Se compara en Java por el mismo motivo que en NacionalidadService (Unicode en SQLite).
    private Optional<FormaDePago> encontrarActiva(TipoPago tipoPago, String observacion) {
        return repository.findByEliminadoFalseOrderByTipoPagoAscObservacionAsc().stream()
                .filter(f -> f.getTipoPago() == tipoPago && f.getObservacion() != null
                        && f.getObservacion().strip().equalsIgnoreCase(observacion.strip()))
                .findFirst();
    }

    public FormaDePago buscarFormaDePago(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La forma de pago no existe o fue eliminada.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La forma de pago no existe o fue eliminada."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarFormaDePago(String id, TipoPago tipoPago, String observacion)
            throws ErrorServiceException {
        FormaDePago formaDePago = buscarFormaDePago(id);
        validar(tipoPago, observacion, id);
        formaDePago.setTipoPago(tipoPago);
        formaDePago.setObservacion(observacion.strip());
        repository.save(formaDePago);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarFormaDePago(String id) throws ErrorServiceException {
        FormaDePago formaDePago = buscarFormaDePago(id);
        formaDePago.setEliminado(true);
        repository.save(formaDePago);
    }

    public List<FormaDePago> listarFormaDePago() {
        return repository.findAllByOrderByTipoPagoAscObservacionAsc();
    }

    public List<FormaDePago> listarFormaDePagoActivo() {
        return repository.findByEliminadoFalseOrderByTipoPagoAscObservacionAsc();
    }

    /** Filas del listado del ABM: tipo y observación de cada forma de pago activa. */
    public List<FilaTablaDTO> listarFilaFormaDePagoActivo() {
        return listarFormaDePagoActivo().stream()
                .map(f -> new FilaTablaDTO(f.getId(), describirFormaDePago(f),
                        List.of(f.getTipoPago().getDescripcion(), f.getObservacion())))
                .toList();
    }

    /** Tipos de pago disponibles, en el orden del enum: valor → descripción. */
    public Map<String, String> listarTipoPago() {
        Map<String, String> tipos = new LinkedHashMap<>();
        for (TipoPago tipo : TipoPago.values()) {
            tipos.put(tipo.name(), tipo.getDescripcion());
        }
        return tipos;
    }

    /** Convierte el valor recibido (por ejemplo, desde un formulario) al enum TipoPago. */
    public TipoPago convertirTipoPago(String tipoPago) throws ErrorServiceException {
        if (tipoPago == null || tipoPago.isBlank()) {
            throw new ErrorServiceException("El tipo de pago es obligatorio.");
        }
        for (TipoPago tipo : TipoPago.values()) {
            if (tipo.name().equals(tipoPago)) {
                return tipo;
            }
        }
        throw new ErrorServiceException("El tipo de pago no es válido.");
    }

    private String describirFormaDePago(FormaDePago formaDePago) {
        return formaDePago.getObservacion() + " (" + formaDePago.getTipoPago().getDescripcion() + ")";
    }
}
