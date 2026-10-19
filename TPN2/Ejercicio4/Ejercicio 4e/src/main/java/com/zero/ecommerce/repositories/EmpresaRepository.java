package com.zero.ecommerce.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zero.ecommerce.entities.Empresa;
import com.zero.ecommerce.entities.enums.TipoEmpresa;

public interface EmpresaRepository extends JpaRepository<Empresa, String> {
    List<Empresa> findAllByOrderByRazonSocialAsc();

    List<Empresa> findByEliminadoFalseOrderByRazonSocialAsc();

    Optional<Empresa> findByIdAndEliminadoFalse(String id);

    Optional<Empresa> findByCuitAndEliminadoFalse(String cuit);

    Optional<Empresa> findFirstByTipoSucursalAndEliminadoFalse(TipoEmpresa tipoSucursal);
}
