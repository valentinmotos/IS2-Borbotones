package com.dulcemayor.controller;

import com.dulcemayor.service.NewsletterService;
import com.dulcemayor.service.NewsletterService.ResultadoEnvio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {
    private final NewsletterService newsletter;

    @PostMapping("/enviar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResultadoEnvio enviar() {
        return newsletter.enviarOfertas();
    }
}
