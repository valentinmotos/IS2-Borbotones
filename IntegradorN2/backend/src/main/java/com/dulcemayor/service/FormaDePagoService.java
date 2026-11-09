package com.dulcemayor.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dulcemayor.dto.FormaDePagoRequest;
import com.dulcemayor.dto.FormaDePagoResponse;
import com.dulcemayor.dto.mapper.FormaDePagoMapper;
import com.dulcemayor.entity.FormaDePago;
import com.dulcemayor.entity.enums.TipoPago;
import com.dulcemayor.exception.DuplicadoException;
import com.dulcemayor.exception.ErrorServiceException;
import com.dulcemayor.repository.FormaDePagoRepository;

@Service
@Transactional(readOnly = true)
public class FormaDePagoService extends AbstractCrudService<FormaDePago, FormaDePagoRequest, FormaDePagoResponse> {

    private final FormaDePagoRepository formaRepository;

    public FormaDePagoService(FormaDePagoRepository repositorio, FormaDePagoMapper mapper) {
        super(repositorio, mapper);
        this.formaRepository = repositorio;
    }

    @Transactional public FormaDePagoResponse crearFormaDePago(FormaDePagoRequest request) { return crear(request); }
    @Transactional public FormaDePagoResponse modificarFormaDePago(String id, FormaDePagoRequest request) { return modificar(id, request); }
    @Transactional public void eliminarFormaDePago(String id) { eliminar(id); }
    public FormaDePagoResponse buscarFormaDePago(String id) { return buscar(id); }
    public List<FormaDePagoResponse> listarFormaDePagoActivo() { return listarActivos(); }

    public List<FormaDePagoResponse> listarTodas() {
        return mapper.aRespuestas(formaRepository.findAll(Sort.by("tipoPago").and(Sort.by("observacion"))));
    }

    @Transactional
    public FormaDePagoResponse activar(String id) {
        FormaDePago forma = formaRepository.findById(id)
                .orElseThrow(() -> new com.dulcemayor.exception.RecursoNoEncontradoException("No se encontró la forma de pago."));
        if (!forma.isEliminado()) return mapper.aRespuesta(forma);
        validar(new FormaDePagoRequest(forma.getTipoPago(), forma.getObservacion()), id);
        forma.setEliminado(false);
        return mapper.aRespuesta(formaRepository.save(forma));
    }

    @Override
    protected void validar(FormaDePagoRequest request, String idActual) {
        if (request == null || request.tipoPago() == null) {
            throw new ErrorServiceException("El tipo de pago es obligatorio.", "tipoPago");
        }
        String observacion = request.observacion() == null ? "" : request.observacion().strip();
        if (request.tipoPago() == TipoPago.TRANSFERENCIA && observacion.isBlank()) {
            throw new ErrorServiceException("El alias es obligatorio para la transferencia.", "observacion");
        }
        boolean duplicada = formaRepository.findByEliminadoFalse().stream()
                .filter(f -> !f.getId().equals(idActual))
                .anyMatch(f -> f.getTipoPago() == request.tipoPago()
                        && java.util.Objects.equals(normalizar(f.getObservacion()), normalizar(observacion)));
        if (duplicada) throw new DuplicadoException("Esa forma de pago ya está registrada.", "tipoPago");
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.strip().toLowerCase(java.util.Locale.ROOT);
    }

    @Override protected FormaDePago nuevaEntidad() { return new FormaDePago(); }

    @Override
    protected void asignar(FormaDePago forma, FormaDePagoRequest request) {
        forma.setTipoPago(request.tipoPago());
        String observacion = request.observacion() == null ? null : request.observacion().strip();
        forma.setObservacion(observacion == null || observacion.isBlank() ? null : observacion);
    }

    @Override protected String mensajeNoEncontrado() { return "No se encontró la forma de pago."; }
    @Override protected Sort orden() { return Sort.by("tipoPago").and(Sort.by("observacion")); }
}
