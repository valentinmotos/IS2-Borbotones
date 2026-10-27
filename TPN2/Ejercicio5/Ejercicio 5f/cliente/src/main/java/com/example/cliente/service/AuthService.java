package com.example.cliente.service;

import com.example.cliente.dao.AuthDAO;
import com.example.cliente.dto.LoginDTO;
import com.example.cliente.dto.TokenDTO;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthDAO authDAO;

    public AuthService(AuthDAO authDAO) {
        this.authDAO = authDAO;
    }

    public TokenDTO login(LoginDTO login) {
        return authDAO.login(login);
    }
}
