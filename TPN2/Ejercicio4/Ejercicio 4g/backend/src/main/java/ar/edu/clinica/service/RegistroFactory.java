package ar.edu.clinica.service;
import ar.edu.clinica.model.*;
import java.util.UUID;
/** Factory Method: cada creador concreto decide qué registro instanciar. */
public abstract class RegistroFactory<T extends Registro> {
 protected abstract T instanciar();
 public final T crear() { T r=instanciar(); r.id=UUID.randomUUID().toString(); r.eliminado=false; return r; }
}
