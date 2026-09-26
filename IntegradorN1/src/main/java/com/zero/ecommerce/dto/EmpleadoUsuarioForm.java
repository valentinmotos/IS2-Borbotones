package com.zero.ecommerce.dto;

import com.zero.ecommerce.entities.Empleado;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos editables de un empleado y de su cuenta. La clave nunca se precarga. */
@Getter
@Setter
@NoArgsConstructor
public class EmpleadoUsuarioForm {
    private String nombre;
    private String apellido;
    private String fechaNacimiento;
    private String tipoDocumento;
    private String numeroDocumento;
    private String correo;
    private String rol;
    private String clave;
    private String confirmacionClave;

    public static EmpleadoUsuarioForm desde(Empleado empleado) {
        EmpleadoUsuarioForm form = new EmpleadoUsuarioForm();
        form.setNombre(empleado.getNombre());
        form.setApellido(empleado.getApellido());
        form.setFechaNacimiento(empleado.getFechaNacimiento() == null ? "" : empleado.getFechaNacimiento().toString());
        form.setTipoDocumento(empleado.getTipoDocumento() == null ? "" : empleado.getTipoDocumento().name());
        form.setNumeroDocumento(empleado.getNumeroDocumento());
        if (empleado.getUsuario() != null) {
            form.setCorreo(empleado.getUsuario().getNombreUsuario());
            form.setRol(empleado.getUsuario().getRol().name());
        }
        return form;
    }
}
