package com.dulcemayor.repository;

import com.dulcemayor.entity.ContactoCorreoElectronico;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoCorreoElectronicoRepository extends JpaRepository<ContactoCorreoElectronico, String> {
    List<ContactoCorreoElectronico> findByMinorista_IdAndEliminadoFalseOrderByIdAsc(String minoristaId);
}
