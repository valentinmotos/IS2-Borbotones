package com.borbotones.integrador1.services;

import com.borbotones.integrador1.entities.Producto;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MercadoPagoService {

    @PostConstruct
    public void configurarMercadoPago() {
        MercadoPagoConfig.setAccessToken( "APP_USR-1125880501822374-092203-904f8be1bc8af2ec9f9cb78075cff7f2-3014215549" );
    }

    public String generarOrdenPagoMP(List<Producto> productos) {
        try{
            List<PreferenceItemRequest> items = new ArrayList<>();

            PreferenceItemRequest producto1 =
                    PreferenceItemRequest.builder()
                            .title("Producto 1")
                            .quantity(2)
                            .unitPrice(new BigDecimal("5000"))
                            .currencyId("ARS")
                            .build();

            items.add(producto1);

            PreferenceRequest request =
                    PreferenceRequest.builder()
                            .items(items)
                            .build();

            PreferenceClient client = new PreferenceClient();

            Preference preference = client.create(request);

            return preference.getId();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException( "Error al crear la preferencia de Mercado Pago",  e );
        }

    }
}