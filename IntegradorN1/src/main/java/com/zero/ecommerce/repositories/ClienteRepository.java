package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    Optional<Cliente> findByIdAndEliminadoFalse(String id);

    Optional<Cliente> findByUsuario_IdAndEliminadoFalse(String idUsuario);

    List<Cliente> findAllByOrderByApellidoAscNombreAsc();

    List<Cliente> findByEliminadoFalseOrderByApellidoAscNombreAsc();
}
