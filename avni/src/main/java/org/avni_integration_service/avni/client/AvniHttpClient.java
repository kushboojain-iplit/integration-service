package org.avni_integration_service.avni.client;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.auth.IdpDetailsResponse;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class AvniHttpClient {
    private final RestTemplate restTemplate;

    private static final Logger logger = Logger.getLogger(AvniHttpClient.class);

    private static ThreadLocal<AvniSession> avniSessions = new ThreadLocal<>();

    public AvniHttpClient(RestTemplate restTemplate) {
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build()));
        this.restTemplate = restTemplate;
    }

    public void setAvniSession(AvniSession avniSession) {
        avniSessions.set(avniSession);
    }

    public AvniSession getAvniSession() {
        AvniSession avniSession = avniSessions.get();
        if (avniSession == null)
            throw new IllegalStateException("No Avni connection available. Have you called setAvniConnectionDetails.");
        return avniSession;
    }

    public <T> ResponseEntity<T> get(String path, Map<String, String> queryParams, Class<T> returnType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(path));
        for (var entry : queryParams.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }

        URI uri = builder.build().toUri();
        return getResponseEntity(returnType, uri, HttpMethod.GET, null);
    }

    private <T> ResponseEntity<T> getResponseEntity(Class<T> returnType, URI uri, HttpMethod method, String json) {
        try {
            logger.debug("%s %s".formatted(method.name(), uri.toString()));
            return restTemplate.exchange(uri, method, getRequestEntity(json), returnType);
        } catch (HttpServerErrorException.InternalServerError e) {
            if (e.getMessage().contains("TokenExpiredException")) {
                getAvniSession().clearAuthInformation();
                return restTemplate.exchange(uri, method, getRequestEntity(json), returnType);
            }
            throw e;
        }
    }

    private HttpEntity<String> getRequestEntity(String json) {
        return json == null ? new HttpEntity<>(authHeaders()) : new HttpEntity<>(json, authHeaders());
    }

    public <T> ResponseEntity<T> get(String url, Class<T> returnType) {
        return get(url, new HashMap<>(), returnType);
    }

    public <T, U> ResponseEntity<U> post(String url, T t, Class<U> returnType) {
        logger.info(String.format("POST: %s", url));
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(url));
        String json = ObjectJsonMapper.writeValueAsString(t);
        return getResponseEntity(returnType, builder.build().toUri(), HttpMethod.POST, json);
    }

    public <T, U> ResponseEntity<U> put(String url, T requestBody, Class<U> returnType) {
        logger.info(String.format("PUT: %s", url));
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(url));
        try {
            return restTemplate.exchange(builder.build().toUri(), HttpMethod.PUT, new HttpEntity<>(requestBody, authHeaders()), returnType);
        } catch (HttpServerErrorException.InternalServerError e) {
            if (e.getMessage().contains("TokenExpiredException")) {
                getAvniSession().clearAuthInformation();
                return restTemplate.exchange(builder.build().toUri(), HttpMethod.PUT, new HttpEntity<>(requestBody, authHeaders()), returnType);
            }
            throw e;
        }
    }

    public <T, U> ResponseEntity<U> patch(String url, T requestBody, Class<U> returnType) {
        logger.info(String.format("PATCH: %s", url));
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(url));
        try {
            return restTemplate.exchange(builder.build().toUri(), HttpMethod.PATCH, new HttpEntity<>(requestBody, authHeaders()), returnType);
        } catch (HttpServerErrorException.InternalServerError e) {
            if (e.getMessage().contains("TokenExpiredException")) {
                getAvniSession().clearAuthInformation();
                return restTemplate.exchange(builder.build().toUri(), HttpMethod.PATCH, new HttpEntity<>(requestBody, authHeaders()), returnType);
            }
            throw e;
        }
    }

    public <T> ResponseEntity<T> delete(String url,  Map<String, String> queryParams, String json, Class<T> returnType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(url));
        try {
            for (var entry : queryParams.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            return restTemplate.exchange(builder.build().toUri(), HttpMethod.DELETE,
                    new HttpEntity<>(getRequestEntity(json), authHeaders()), returnType);
        } catch (HttpServerErrorException.InternalServerError e) {
            if (e.getMessage().contains("TokenExpiredException")) {
                getAvniSession().clearAuthInformation();
            }
            throw e;
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (!getAvniSession().getAuthWithAvni()) { //Skip Cognito Auth Token fetch in local
            headers.add("user-name", getAvniSession().getAvniImplUser());
        } else {
            headers.add("auth-token", fetchAuthToken());
        }
        headers.add("content-type", "application/json");
        return headers;
    }

    public String fetchAuthToken() {
        String idToken = getAvniSession().getIdToken();
        if (idToken != null) return idToken;

        RestTemplate restTemplate = new RestTemplate();
        logger.debug("Getting cognito details");
        ResponseEntity<IdpDetailsResponse> response = restTemplate.getForEntity(getAvniSession().apiUrl("/idp-details"), IdpDetailsResponse.class);
        IdpDetailsResponse idpDetailsResponse = response.getBody();
        return getAvniSession().fetchIdToken(idpDetailsResponse);
    }

    public void clearAuthInformation() {
        getAvniSession().clearAuthInformation();
    }

    public String getUri(String url, HashMap<String, String> queryParams) {
        return getAvniSession().getUri(url, queryParams);
    }
}
