package com.debuggeandoideas.mapstruct_example;

import com.debuggeandoideas.mapstruct_example.dao.CountryDao;
import com.debuggeandoideas.mapstruct_example.dtos.CountryDto;
import com.debuggeandoideas.mapstruct_example.mappers.CountryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class CountryService {

    public List<CountryDto> readAll() {
        return CountryDao.db.values().stream()
                .map(CountryMapper.mapper::toCountryDto)
                .sorted(Comparator.comparing(CountryDto::getName))
                .toList();
    }

    public CountryDto readById(UUID id) {

        if (CountryDao.db.containsKey(id)) {
            return CountryMapper.mapper.toCountryDto(CountryDao.db.get(id));
        } else {
            log.error("Country with id {} not found", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Country with id: " + id + " not found");
        }
    }
}
