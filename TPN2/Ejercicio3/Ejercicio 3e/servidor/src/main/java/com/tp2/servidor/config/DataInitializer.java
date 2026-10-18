package com.tp2.servidor.config;

import com.tp2.servidor.dao.ArticuloDao;
import com.tp2.servidor.dao.UsuarioDao;
import com.tp2.servidor.entity.Articulo;
import com.tp2.servidor.entity.Usuario;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UsuarioDao usuarioDao;
    private final ArticuloDao articuloDao;

    public DataInitializer(UsuarioDao usuarioDao, ArticuloDao articuloDao) {
        this.usuarioDao = usuarioDao;
        this.articuloDao = articuloDao;
    }

    @Override
    public void run(String... args) {
        if (usuarioDao.count() == 0) {
            Usuario usuario = new Usuario();
            usuario.setNombre("Cliente Demo");
            usuario.setClave("1234");
            usuarioDao.save(usuario);
        }

        if (articuloDao.count() == 0) {
            crearArticulo("Teclado mecanico", 45000);
            crearArticulo("Mouse optico", 18000);
            crearArticulo("Monitor 24 pulgadas", 135000);
        }
    }

    private void crearArticulo(String nombre, double precio) {
        Articulo articulo = new Articulo();
        articulo.setNombre(nombre);
        articulo.setPrecio(precio);
        articuloDao.save(articulo);
    }
}
