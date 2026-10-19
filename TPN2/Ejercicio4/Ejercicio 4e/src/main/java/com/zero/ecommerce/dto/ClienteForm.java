package com.zero.ecommerce.dto;

import com.zero.ecommerce.entities.Cliente;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del formulario de perfil del cliente. La dirección viaja en un DireccionForm aparte (fragments/direccion)
 * y la foto como MultipartFile. Los enums y la fecha viajan como texto y los convierte ClienteService.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteForm {

    private String nombre;
    private String apellido;
    private String sexo;
    private String fechaNacimiento;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nacionalidadId;
    private String telefono;

    /** Arma el formulario de edición a partir del cliente guardado. */
    public static ClienteForm desde(Cliente cliente) {
        ClienteForm form = new ClienteForm();
        form.setNombre(cliente.getNombre());
        form.setApellido(cliente.getApellido());
        form.setSexo(cliente.getSexo() == null ? null : cliente.getSexo().name());
        form.setFechaNacimiento(cliente.getFechaNacimiento() == null ? null : cliente.getFechaNacimiento().toString());
        form.setTipoDocumento(cliente.getTipoDocumento() == null ? null : cliente.getTipoDocumento().name());
        form.setNumeroDocumento(cliente.getNumeroDocumento());
        form.setNacionalidadId(cliente.getNacionalidad() == null ? null : cliente.getNacionalidad().getId());
        form.setTelefono(cliente.getTelefono() == null ? null : cliente.getTelefono().getTelefono());
        return form;
    }
}
