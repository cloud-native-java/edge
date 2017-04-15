package edge;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.applications.UnsetEnvironmentVariableApplicationRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.RetryCallback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.parseMediaType;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class EdgeIT extends AbstractEdgeTest {

    private Log log = LogFactory.getLog(getClass());

    @Test
    public void restClients() throws Throwable {

        log.info("running restClients()");
        // resttemplate
        baselineDeploy(new String[]{"insecure"},
                Collections.singletonMap("security.basic.enabled", "false"), null,
                new String[]{"insecure"},
                Collections.singletonMap("security.basic.enabled", "false"), null);
        testEdgeRestClient("Shafer", "/api/resttemplate/");

        // feign
        baselineDeploy(new String[]{"insecure"},
                Collections.singletonMap("security.basic.enabled", "false"), null,
                "insecure,feign".split(","),
                Collections.singletonMap("security.basic.enabled", "false"), null);
        testEdgeRestClient("Watters", "/api/feign/");
    }

    @Test
    public void testAuth() throws Throwable {

        log.info("running testAuth()");

        ApplicationInstanceConfiguration callback = (appId) -> {
            String prop = "security.basic.enabled";
            this.cloudFoundryOperations
                    .applications()
                    .unsetEnvironmentVariable(
                            UnsetEnvironmentVariableApplicationRequest.builder().name(appId)
                                    .variableName(prop).build()).block();
            this.restart(appId);
        };

        baselineDeploy(new String[]{"secure"}, new HashMap<>(), callback,
                new String[]{"secure", "sso"}, new HashMap<>(), callback);

        String accessToken = this.obtainToken();

        String userEndpointOnEdgeService = this.service.urlForApplication(this
                .appNameFromManifest(this.greetingsServiceManifest)) + "/greet/OAuth";

        RequestEntity<Void> requestEntity = RequestEntity
                .<String>get(URI.create(userEndpointOnEdgeService))
                .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).build();
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity,
                String.class);
        String body = responseEntity.getBody();
        this.log.info("body from authorized request: " + body);
    }

    @Test
    public void testCors() throws Throwable {
        log.info("running testCors()");
        Map<String, String> e = Collections.singletonMap("security.basic.enabled",
                "false");
        this.baselineDeploy(new String[]{"insecure"}, e, null,
                "cors,insecure".split(","), e, null);
        this.deployAppIfDoesNotExist(this.html5ClientManifest);
        String edgeServiceUri = this.service.urlForApplication(appNameFromManifest(
                this.edgeServiceManifest))
                + "/lets/greet/Phil";
        String html5ClientUri = this.service.urlForApplication(this
                .appNameFromManifest(this.html5ClientManifest));
        this.log.info("edge-service URI " + edgeServiceUri);
        this.log.info("html5-client URI " + html5ClientUri);
        RestTemplate restTemplate = new RestTemplate();
        List<String> headerList = Arrays.asList(ACCEPT, "X-Requested-With", ORIGIN);
        String headersString = StringUtils.arrayToDelimitedString(
                headerList.toArray(), ", ").trim();
        RequestEntity<Void> requestEntity = RequestEntity
                .options(URI.create(edgeServiceUri))
                .header(ACCEPT, parseMediaType("*/*").toString())
                .header(ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.toString())
                .header(ACCESS_CONTROL_REQUEST_HEADERS, headersString)
                .header(REFERER, html5ClientUri).header(ORIGIN, html5ClientUri).build();
        Set<HttpMethod> httpMethods = restTemplate.optionsForAllow(edgeServiceUri);
        httpMethods.forEach(m -> log.info(m));
        ResponseEntity<Void> responseEntity = this.retryTemplate.execute(ctx -> {
            ResponseEntity<Void> exchange = restTemplate.exchange(requestEntity,
                    Void.class);
            if (!exchange.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN))
                throw new RuntimeException("there's no " + ACCESS_CONTROL_ALLOW_ORIGIN
                        + " header present.");
            return exchange;
        });
        HttpHeaders headers = responseEntity.getHeaders();
        headers.forEach((k, v) -> log.info(k + '=' + v.toString()));
        log.info("response received: " + responseEntity.toString());
        Assert.assertTrue("preflight response should contain "
                + ACCESS_CONTROL_ALLOW_ORIGIN, headers.containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private String obtainToken() throws Exception {

        String authServiceAppId = this.appNameFromManifest(this.authServiceManifest);

        URI uri = URI.create(this.service.urlForApplication(authServiceAppId)
                + "/uaa/oauth/token");
        String username = "jlong";
        String password = "spring";
        String clientSecret = "password";
        String client = "html5";

        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>() {
            {
                this.add("client_secret", clientSecret);
                this.add("client_id", client);
                this.add("scope", "openid");
                this.add("grant_type", "password");
                this.add("username", username);
                this.add("password", password);
            }
        };

        String token = Base64Utils.encodeToString((client + ":" + clientSecret)
                .getBytes(Charset.forName("UTF-8")));

        RequestEntity<LinkedMultiValueMap<String, String>> requestEntity = RequestEntity
                .post(uri).accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + token).body(map);
        ParameterizedTypeReference<Map<String, String>> type = new ParameterizedTypeReference<Map<String, String>>() {
        };
        String accessToken = this.retryTemplate.execute((ctx) -> {
            ResponseEntity<Map<String, String>> responseEntity = this.restTemplate
                    .exchange(requestEntity, type);
            Map<String, String> body = responseEntity.getBody();
            return body.get("access_token");
        });
        log.info("access_token: " + accessToken);
        return accessToken;
    }

    private void testEdgeRestClient(String testName, String urlSuffix)
            throws Throwable {
        String root = this.service
                .urlForApplication(appNameFromManifest(this.edgeServiceManifest));
        String edgeServiceUrl = root + urlSuffix + testName;
        String healthUrl = root + "/health";
        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(
                healthUrl, String.class);
        log.info("health endpoint: " + responseEntity.getBody());
        String body = retryTemplate
                .execute((RetryCallback<String, Throwable>) context -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(edgeServiceUrl,
                            String.class);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        String msg = "couldn't get a valid response calling the edge service ";
                        this.log.info(msg);
                        throw new RuntimeException(msg + edgeServiceUrl);
                    }
                    return response.getBody();
                });
        Assert.assertTrue(body.contains("Hello, " + testName));
    }


}
