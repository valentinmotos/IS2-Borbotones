package com.zero.ecommerce.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zero.ecommerce.entities.Direccion;
import com.zero.ecommerce.entities.Localidad;
import com.zero.ecommerce.exception.ErrorServiceException;
import com.zero.ecommerce.repositories.DireccionRepository;
import com.zero.ecommerce.utils.TextoUtils;

@Service
@Transactional(readOnly = true)
public class DireccionService {

    private static final int LARGO_MAXIMO_CAMPO = 100;
    private static final int LARGO_MAXIMO_NUMERACION = 10;
    private static final int LARGO_MAXIMO_REFERENCIA = 255;

    private final DireccionRepository repository;
    private final LocalidadService localidadService;

    public DireccionService(DireccionRepository repository, LocalidadService localidadService) {
        this.repository = repository;
        this.localidadService = localidadService;
    }

    /** Devuelve la dirección creada, como indica el diagrama, para asociarla a una persona o empresa. */
    @Transactional(rollbackFor = ErrorServiceException.class)
    public Direccion crearDireccion(String calle, String numeracion, String barrio, String manzanaPiso,
            String casaDepartamento, String referencia, String idLocalidad) throws ErrorServiceException {
        validar(calle, numeracion, barrio, manzanaPiso, casaDepartamento, referencia, idLocalidad);
        Direccion direccion = new Direccion();
        asignar(direccion, calle, numeracion, barrio, manzanaPiso, casaDepartamento, referencia,
                localidadService.buscarLocalidad(idLocalidad));
        return repository.save(direccion);
    }

    public void validar(String calle, String numeracion, String barrio, String manzanaPiso,
            String casaDepartamento, String referencia, String idLocalidad) throws ErrorServiceException {
        validarObligatorio(calle, "La calle es obligatoria.");
        validarLargo(calle, LARGO_MAXIMO_CAMPO, "La calle");
        validarObligatorio(numeracion, "La numeración es obligatoria.");
        validarLargo(numeracion, LARGO_MAXIMO_NUMERACION, "La numeración");
        validarLargo(barrio, LARGO_MAXIMO_CAMPO, "El barrio");
        validarLargo(manzanaPiso, LARGO_MAXIMO_CAMPO, "La manzana o piso");
        validarLargo(casaDepartamento, LARGO_MAXIMO_CAMPO, "La casa o departamento");
        validarLargo(referencia, LARGO_MAXIMO_REFERENCIA, "La referencia");
        validarObligatorio(idLocalidad, "La localidad es obligatoria.");
        localidadService.buscarLocalidad(idLocalidad);
    }

    private void validarObligatorio(String valor, String mensaje) throws ErrorServiceException {
        if (valor == null || valor.isBlank()) {
            throw new ErrorServiceException(mensaje);
        }
    }

    private void validarLargo(String valor, int largoMaximo, String campo) throws ErrorServiceException {
        if (valor != null && valor.strip().length() > largoMaximo) {
            throw new ErrorServiceException(campo + " no puede superar los " + largoMaximo + " caracteres.");
        }
    }

    private void asignar(Direccion direccion, String calle, String numeracion, String barrio, String manzanaPiso,
            String casaDepartamento, String referencia, Localidad localidad) {
        direccion.setCalle(calle.strip());
        direccion.setNumeracion(numeracion.strip());
        direccion.setBarrio(opcional(barrio));
        direccion.setManzanaPiso(opcional(manzanaPiso));
        direccion.setCasaDepartamento(opcional(casaDepartamento));
        direccion.setReferencia(opcional(referencia));
        direccion.setLocalidad(localidad);
    }

    // Los campos opcionales vacíos se guardan como null, no como "".
    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    public Direccion buscarDireccion(String id) throws ErrorServiceException {
        if (id == null || id.isBlank()) {
            throw new ErrorServiceException("La dirección no existe o fue eliminada.");
        }
        return repository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ErrorServiceException("La dirección no existe o fue eliminada."));
    }

    public Direccion buscarDireccionPorCalleNumeracion(String calle, String numeracion)
            throws ErrorServiceException {
        validarObligatorio(calle, "La calle es obligatoria.");
        validarObligatorio(numeracion, "La numeración es obligatoria.");
        return repository.findByNumeracionAndEliminadoFalse(numeracion.strip()).stream()
                .filter(d -> TextoUtils.mismoNombre(d.getCalle(), calle))
                .findFirst()
                .orElseThrow(() -> new ErrorServiceException("No hay una dirección con esa calle y numeración."));
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void modificarDireccion(String id, String calle, String numeracion, String barrio, String manzanaPiso,
            String casaDepartamento, String referencia, String idLocalidad) throws ErrorServiceException {
        Direccion direccion = buscarDireccion(id);
        validar(calle, numeracion, barrio, manzanaPiso, casaDepartamento, referencia, idLocalidad);
        asignar(direccion, calle, numeracion, barrio, manzanaPiso, casaDepartamento, referencia,
                localidadService.buscarLocalidad(idLocalidad));
        repository.save(direccion);
    }

    @Transactional(rollbackFor = ErrorServiceException.class)
    public void eliminarDireccion(String id) throws ErrorServiceException {
        Direccion direccion = buscarDireccion(id);
        direccion.setEliminado(true);
        repository.save(direccion);
    }
}
