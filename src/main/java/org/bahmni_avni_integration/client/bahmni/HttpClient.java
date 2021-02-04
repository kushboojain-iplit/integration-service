package org.bahmni_avni_integration.client.bahmni;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bahmni_avni_integration.client.OpenMRSWebClient;
import org.bahmni_avni_integration.util.ObjectJsonMapper;

import java.io.IOException;
import java.net.URI;

public class HttpClient {
    private Authenticator authenticator;
    private HttpClientInternal httpClientInternal;
    private static Logger logger = Logger.getLogger(HttpClient.class);

    public HttpClient(ConnectionDetails connectionDetails) {
        this(new HttpClientInternal(connectionDetails.getConnectionTimeout(), connectionDetails.getReadTimeout()));
    }

    public HttpClient(ConnectionDetails connectionDetails, Authenticator authenticator) {
        this(new HttpClientInternal(connectionDetails.getConnectionTimeout(), connectionDetails.getReadTimeout(), connectionDetails.getConnectionManager()), authenticator);
    }

    //Just for tests
    public HttpClient(HttpClientInternal httpClientInternal) {
        this(httpClientInternal, new NullAuthenticator());
    }

    //Just for tests
    public HttpClient(HttpClientInternal httpClientInternal, Authenticator authenticator) {
        this.httpClientInternal = httpClientInternal;
        this.authenticator = authenticator;
    }

    public ClientCookies getCookies(URI uri) {
        return authenticator.getRequestDetails(uri).getClientCookies();
    }

    public String get(URI uri) {
        return get(uri, new HttpHeaders());
    }

    private String get(URI uri, HttpHeaders httpHeaders) {
        try {
            HttpResponse httpResponse = httpClientInternal.get(authenticator.getRequestDetails(uri), httpHeaders);

            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED ||
                    httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                httpClientInternal.closeConnection();
                httpClientInternal = httpClientInternal.createNew();
                httpResponse = httpClientInternal.get(authenticator.refreshRequestDetails(uri), httpHeaders);
            }

            checkSanityOfResponse(httpResponse);
            return asString(httpResponse);
        } finally {
            httpClientInternal.closeConnection();
        }
    }

    public <T> T get(String url, Class<T> returnType) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("Accept", "application/json");
        String response = get(URI.create(url), httpHeaders);
        return ObjectJsonMapper.readValue(response, returnType);
    }

    private void checkSanityOfResponse(HttpResponse httpResponse) {
        StatusLine statusLine = httpResponse.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode < 200 || statusCode >= 300) throw new WebClientsException("Bad response code of " + statusCode);

        HttpEntity entity = httpResponse.getEntity();
        if (entity == null) throw new WebClientsException("Cannot read response");
    }

    private String asString(HttpResponse httpResponse) {
        try {
            return EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void post(String path, String json) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("Accept", "application/json");
        logger.info("Posting to: " + path);
        HttpResponse httpResponse = httpClientInternal.post(authenticator.getRequestDetails(URI.create(path)), httpHeaders, json);
        if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
            logger.error(asString(httpResponse));
            throw new RuntimeException("Post failed");
        }
    }
}
