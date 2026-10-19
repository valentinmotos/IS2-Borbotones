package ar.edu.clinica.service;
import org.springframework.http.HttpStatus;
public class NegocioException extends RuntimeException {
 public final HttpStatus estado;
 public NegocioException(HttpStatus estado,String mensaje) { super(mensaje); this.estado=estado; }
 public static NegocioException invalido(String mensaje) { return new NegocioException(HttpStatus.BAD_REQUEST,mensaje); }
}
