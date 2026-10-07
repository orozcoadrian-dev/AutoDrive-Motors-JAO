package com.autodrive.motors.external;

import com.autodrive.motors.config.TasaCambioProperties;
import com.autodrive.motors.exception.ServicioExternoNoDisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
public class ExchangeRateApiClient implements TasaCambioClient {
    private final RestClient restClient;
    private final String url;

    public ExchangeRateApiClient(RestClient.Builder builder, TasaCambioProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        this.restClient = builder.requestFactory(requestFactory).build();
        this.url = properties.getUrl();
    }

    @Override
    public BigDecimal obtenerCopPorUsd() {
        try {
            JsonNode respuesta = restClient.get().uri(url).retrieve().body(JsonNode.class);
            JsonNode tasa = respuesta == null ? null : respuesta.path("rates").path("COP");
            if (tasa == null || !tasa.isNumber() || tasa.decimalValue().signum() <= 0) {
                throw new ServicioExternoNoDisponibleException("El servicio de tasa de cambio no entregó una tasa COP válida.");
            }
            return tasa.decimalValue();
        } catch (RestClientException ex) {
            throw new ServicioExternoNoDisponibleException("No fue posible consultar la tasa de cambio.", ex);
        }
    }
}
