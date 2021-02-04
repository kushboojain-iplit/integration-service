package org.bahmni_avni_integration.client.bahmni.openmrs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bahmni_avni_integration.client.bahmni.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class OpenMRSLoginAuthenticator implements Authenticator {
    private static Logger logger = Logger.getLogger(OpenMRSLoginAuthenticator.class);
    private final String SESSION_ID_KEY = "JSESSIONID";
    CloseableHttpClient httpClient;

    private ConnectionDetails authenticationDetails;
    private HttpRequestDetails previousSuccessfulRequest;

    public OpenMRSLoginAuthenticator(ConnectionDetails authenticationDetails) {
        this.authenticationDetails = authenticationDetails;
    }

    @Override
    public HttpRequestDetails getRequestDetails(URI uri) {
        if (previousSuccessfulRequest == null) {
            return refreshRequestDetails(uri);
        }
        return previousSuccessfulRequest.createNewWith(uri);
    }

    @Override
    public HttpRequestDetails refreshRequestDetails(URI uri) {
        String responseText = null;

        try {

            HttpGet httpGet = new HttpGet(authenticationDetails.getAuthUrl());
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(authenticationDetails.getConnectionTimeout())
                    .setSocketTimeout(authenticationDetails.getReadTimeout())
                    .setConnectionRequestTimeout(authenticationDetails.getReadTimeout())
                    .build();
            httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(requestConfig)
                    .build();

            setCredentials(httpGet);

            logger.info(String.format("Executing request: %s", httpGet.getRequestLine()));

            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if(response.getStatusLine().getStatusCode() ==204) {
                throw new WebClientsException("Two factor authentication is enabled, Please enable required privilege for the user");
            }
            if (entity != null) {
                InputStream content = entity.getContent();
                responseText = IOUtils.toString(content);
            }
            logger.info(String.format("Authentication response: %s", responseText));
            EntityUtils.consume(entity);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            OpenMRSAuthenticationResponse openMRSResponse = objectMapper.readValue(responseText, OpenMRSAuthenticationResponse.class);
            confirmAuthenticated(openMRSResponse);

            ClientCookies clientCookies = new ClientCookies();
            clientCookies.put(SESSION_ID_KEY, openMRSResponse.getSessionId());

            previousSuccessfulRequest = new HttpRequestDetails(uri, clientCookies, new HttpHeaders());
            return previousSuccessfulRequest;

        } catch (Exception e) {
            throw new WebClientsException(e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void setCredentials(HttpGet httpGet) throws AuthenticationException {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(authenticationDetails.getUserId(), authenticationDetails.getPassword());
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());
        context.setCredentialsProvider(credsProvider);
        Header authorizationHeader = new BasicScheme(StandardCharsets.UTF_8).authenticate(credentials , httpGet, context);
       // Header authorizationHeader = scheme.authenticate(credentials, httpGet);
        httpGet.setHeader(authorizationHeader);
    }

    private void confirmAuthenticated(OpenMRSAuthenticationResponse openMRSResponse) {
        if (!openMRSResponse.isAuthenticated()) {
            logger.error("Could not authenticate with OpenMRS. ");
            throw new WebClientsException("Could not authenticate with OpenMRS");
        }
    }
}