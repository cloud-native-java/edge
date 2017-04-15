package edge;

import cnj.CloudFoundryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.cloudfoundry.operations.applications.UnsetEnvironmentVariableApplicationRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.ThreadWaitSleeper;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class PasswordGrantIT {


    @Autowired
    protected CloudFoundryOperations cloudFoundryOperations;

    @Autowired
    protected CloudFoundryService service;

    protected File root, authServiceManifest, eurekaManifest, edgeServiceManifest,
            greetingsServiceManifest, html5ClientManifest;


    protected void deployHtml5Client() throws Throwable {
        String html5AppId = this.appNameFromManifest(this.html5ClientManifest);
        if (!this.service.applicationExists(html5AppId)) {
            this.service.pushApplicationUsingManifest(this.html5ClientManifest);
            this.log.info("deployed " + html5AppId);
        }
    }

    protected String deployAuthService() throws Throwable {
        String authService = this.appNameFromManifest(this.authServiceManifest);
        if (!this.service.applicationExists(authService)) {
            this.service
                    .pushApplicationAndCreateUserDefinedServiceUsingManifest(this.authServiceManifest);
            this.log.info("deployed " + authService);
        }
        return authService;
    }

    protected void setEnvironmentVariable(String appId, String k, String v) {
        this.cloudFoundryOperations
                .applications()
                .setEnvironmentVariable(
                        SetEnvironmentVariableApplicationRequest.builder().name(appId)
                                .variableName(k).variableValue(v).build()).block();
    }

    protected void reconfigureApplicationProfile(String appId, String profiles[]) {
        String profileVarName = "spring_profiles_active".toUpperCase();
        String profilesString = StringUtils
                .arrayToCommaDelimitedString(profiles(profiles));
        this.log.info("going to set the env var " + profileVarName + " to value "
                + profilesString + " for the application " + appId);
        this.setEnvironmentVariable(appId, profileVarName, profilesString);
        restart(appId);
    }

    protected void restart(String appId) {
        this.cloudFoundryOperations.applications()
                .restart(RestartApplicationRequest.builder().name(appId).build()).block();
    }

    protected String deployEurekaService() throws Throwable {
        return this.service
                .applicationManifestFrom(this.eurekaManifest)
                .entrySet()
                .stream()
                .map(
                        e -> {
                            String appId = e.getValue().getName();
                            if (!this.service.applicationExists(appId))
                                service.pushApplicationAndCreateUserDefinedServiceUsingManifest(
                                        e.getKey(), e.getValue());
                            return appId;
                        }).findAny().orElse(null);
    }

    protected static String[] profiles(String... profiles) {
        Collection<String> p = new ArrayList<>();
        if (null != profiles && 0 != profiles.length) {
            p.addAll(Arrays.asList(profiles));
        }
        p.add("cloud");
        return p.toArray(new String[p.size()]);
    }

    protected String deployGreetingsService() throws Throwable {
        return this.service.applicationManifestFrom(this.greetingsServiceManifest)
                .entrySet().stream().map(e -> {
                    File f = e.getKey();
                    ApplicationManifest am = e.getValue();
                    String appId = am.getName();
                    if (!this.service.applicationExists(appId))
                        this.service.pushApplicationUsingManifest(f, am, false);
                    return appId;
                }).findAny().orElse(null);
    }

    protected void defaultSetup(boolean delete) throws Throwable {
        this.root = new File(".");
        this.authServiceManifest = new File(root, "../auth-service/manifest.yml");
        this.eurekaManifest = new File(root, "../service-registry/manifest.yml");
        this.edgeServiceManifest = new File(root, "../edge-service/manifest.yml");
        this.greetingsServiceManifest = new File(root,
                "../greetings-service/manifest.yml");
        this.html5ClientManifest = new File(root, "../html5-client/manifest.yml");

        Assert.assertTrue(this.authServiceManifest.exists());
        Assert.assertTrue(this.html5ClientManifest.exists());
        Assert.assertTrue(this.greetingsServiceManifest.exists());
        Assert.assertTrue(this.eurekaManifest.exists());
        Assert.assertTrue(this.edgeServiceManifest.exists());

        String authServiceAppId = this.appNameFromManifest(this.authServiceManifest);
        String eurekaAppId = this.appNameFromManifest(this.eurekaManifest);
        String html5AppId = this.appNameFromManifest(this.html5ClientManifest);
        String edgeServiceAppId = this.appNameFromManifest(this.edgeServiceManifest);
        String greetingsServiceAppId = this
                .appNameFromManifest(this.greetingsServiceManifest);

        if (delete) {
            Stream.of(html5AppId, edgeServiceAppId, greetingsServiceAppId, eurekaAppId,
                    authServiceAppId).forEach(appId -> {
                this.service.destroyApplicationIfExists(appId);
                this.log.info("deleted application " + appId);
            });

            Stream.of(eurekaAppId, authServiceAppId).forEach(svcId -> {
                this.service.destroyServiceIfExists(svcId);
                log.info("deleted service " + svcId);
            });
        }
    }

    protected String appNameFromManifest(File a) {
        return this.service.applicationManifestFrom(a).entrySet().stream()
                .map(e -> e.getValue().getName()).findAny().orElse(null);
    }

    public interface ApplicationInstanceConfiguration {

        void configure(String appId);
    }


    protected void baselineDeploy(

            // greetings-service
            String[] gsProfiles, Map<String, String> gsEnv,
            ApplicationInstanceConfiguration gsCallback,

            // edge-service
            String[] esProfiles, Map<String, String> esEnv,
            ApplicationInstanceConfiguration esCallback

    ) throws Throwable {

        // eureka
        String eurekaServiceId = this.deployEurekaService();
        // auth
        String authServiceId = this.deployAuthService();
        // greetings
        String greetingsServiceId = this.deployGreetingsService();
        if (null != gsCallback) {
            gsCallback.configure(greetingsServiceId);
        }

        this.reconfigureApplicationProfile(greetingsServiceId, gsProfiles);
        gsEnv
                .forEach((k, v) -> this.setEnvironmentVariable(greetingsServiceId, k, v));
        this.restart(greetingsServiceId);
        this.log.info("deployed " + greetingsServiceId);

        // edge
        String edgeServiceId = this.deployEdgeService();
        if (null != esCallback)
            esCallback.configure(edgeServiceId);
        this.reconfigureApplicationProfile(edgeServiceId, esProfiles);
        esEnv.forEach((k, v) -> this.setEnvironmentVariable(edgeServiceId, k, v));
        this.restart(edgeServiceId);
        this.log.info("deployed " + edgeServiceId);
    }

    protected String deployEdgeService() {
        return this.service.applicationManifestFrom(this.edgeServiceManifest)
                .entrySet().stream().map(e -> {
                    File f = e.getKey();
                    ApplicationManifest am = e.getValue();
                    String appId = am.getName();
                    if (!this.service.applicationExists(appId))
                        this.service.pushApplicationUsingManifest(f, am, false);
                    return appId;
                }).findAny().orElse(null);

    }


    //////////////////////////////////
    private Log log = LogFactory.getLog(getClass());

    private final RetryTemplate retryTemplate = retryTemplate();

    private static RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(30 * 1000);
        backOffPolicy.setMaxInterval(90 * 1000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @Before
    public void before() throws Throwable {
        this.defaultSetup(true);
    }

    @Test
    public void testAuth() throws Throwable {

        ApplicationInstanceConfiguration callback = (appId) -> {
            String prop = "security.basic.enabled";
            this.cloudFoundryOperations
                    .applications()
                    .unsetEnvironmentVariable(
                            UnsetEnvironmentVariableApplicationRequest.builder().name(appId)
                                    .variableName(prop).build()).block();
            this.restart(appId);
        };

        baselineDeploy(new String[]{"secure"}, new HashMap<String, String>(), callback,
                new String[]{"secure", "sso"}, new HashMap<String, String>(), callback);

        String accessToken = this.obtainToken();

        String userEndpointOnEdgeService = this.service.urlForApplication(this
                .appNameFromManifest(this.greetingsServiceManifest)) + "/greet/OAuth";
        // d259fbcd-56a8-49f7-9954-c969276219f1
        RequestEntity<Void> requestEntity = RequestEntity
                .<String>get(URI.create(userEndpointOnEdgeService))
                .header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).build();
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity,
                String.class);
        String body = responseEntity.getBody();
        this.log.info("body from authorized request: " + body);
    }

    private String obtainToken() throws Exception {

        String authServiceAppId = this.appNameFromManifest(this.authServiceManifest);

        URI uri = URI.create(this.service.urlForApplication(authServiceAppId) + "/uaa/oauth/token");
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

        ParameterizedTypeReference<Map<String, String>> type =
                new ParameterizedTypeReference<Map<String, String>>() {
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
}
