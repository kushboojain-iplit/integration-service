package org.avni_integration_service.bahmni.client.openmrs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.avni_integration_service.bahmni.client.Authenticator;
import org.avni_integration_service.bahmni.client.ClientCookies;
import org.avni_integration_service.bahmni.client.ConnectionDetails;
import org.avni_integration_service.bahmni.client.HttpHeaders;
import org.avni_integration_service.bahmni.client.HttpRequestDetails;
import org.avni_integration_service.bahmni.client.WebClientsException;

import java.io.InputStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenMRSLoginAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(OpenMRSLoginAuthenticator.class);
    private final String SESSION_ID_KEY = "JSESSIONID";

    private static final ObjectMapper objectMapper = new ObjectMapper();


    private ConnectionDetails authenticationDetails;
    private HttpRequestDetails previousSuccessfulRequest;

    public OpenMRSLoginAuthenticator(ConnectionDetails authenticationDetails) {
        this.authenticationDetails = authenticationDetails;
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, authenticationDetails.getReadTimeout());
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, authenticationDetails.getConnectionTimeout());

            HttpGet httpGet = new HttpGet(authenticationDetails.getAuthUrl());

            setCredentials(httpGet);

            logger.info(String.format("Executing request: %s", httpGet.getRequestLine()));

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if(response.getStatusLine().getStatusCode() ==204) {
                throw new WebClientsException("Two factor authentication is enabled, Please enable required privilege for the user");
            }
            if (entity != null) {
                InputStream content = entity.getContent();
                responseText = IOUtils.toString(content);
            }
            logger.debug(String.format("Authentication response: %s", responseText));
            EntityUtils.consume(entity);
            OpenMRSAuthenticationResponse openMRSResponse = objectMapper.readValue(responseText, OpenMRSAuthenticationResponse.class);
            confirmAuthenticated(openMRSResponse);

            ClientCookies clientCookies = new ClientCookies();
            clientCookies.put(SESSION_ID_KEY, ExtractStringUsingRegex(response.getHeaders("Set-Cookie")[0].getValue()));

            previousSuccessfulRequest = new HttpRequestDetails(uri, clientCookies, new HttpHeaders());
            return previousSuccessfulRequest;

        } catch (Exception e) {
            throw new WebClientsException(e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private String ExtractStringUsingRegex(String Cookie){
        if (Cookie == null) return null;
        Pattern pattern = Pattern.compile("\\bJSESSIONID=([A-Z0-9]{32})");
        Matcher matcher = pattern.matcher(Cookie);
        if (matcher.find()) return matcher.group(1);
        throw new WebClientsException("No Matching SessionID in the Response Cookie");
    }

    protected void setCredentials(HttpGet httpGet) throws AuthenticationException {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(authenticationDetails.getUserId(), authenticationDetails.getPassword());
        BasicScheme scheme = new BasicScheme();
        Header authorizationHeader = scheme.authenticate(credentials, httpGet);
        httpGet.setHeader(authorizationHeader);
    }

    private void confirmAuthenticated(OpenMRSAuthenticationResponse openMRSResponse) {
        if (!openMRSResponse.isAuthenticated()) {
            logger.error("Could not authenticate with OpenMRS. ");
            throw new WebClientsException("Could not authenticate with OpenMRS");
        }
    }
}
