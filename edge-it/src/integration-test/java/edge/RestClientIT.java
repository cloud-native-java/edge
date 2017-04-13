package edge;

import cnj.CloudFoundryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;

/**
 * This IT tests that our client (in the
 * `edge-service`) can call the
 * downstream `greeting-service` through
 * a {@code RestTemplate} client or
 * through a Feign-based REST client.
 * Both should enjoy client-side
 * load-balancing thanks to Netflix
 * Ribbon.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class RestClientIT {

    @Autowired
    private CloudFoundryOperations cloudFoundryOperations;

    @Autowired
    private CloudFoundryService service;

    private final RestTemplate rt = new RestTemplate();
    private File eurekaManifest, edgeServiceManifest, greetingsServiceManifest;

    private Log log = LogFactory.getLog(getClass());

    private void setEnvironmentVariable(String appId, String k, String v) {

        this.cloudFoundryOperations
                .applications()
                .setEnvironmentVariable(
                        SetEnvironmentVariableApplicationRequest
                                .builder()
                                .name(appId)
                                .variableName(k)
                                .variableValue(v)
                                .build())
                .block();

    }

    private void reconfigureApplicationProfile(String appId, String profiles[]) {

        String profileVarName = "spring_profiles_active".toUpperCase();

        String profilesString = StringUtils.arrayToCommaDelimitedString(profiles(profiles));
        this.log.info("going to set the env var " + profileVarName + " to value "
                + profilesString + " for the application " + appId);

        this.setEnvironmentVariable(appId, profileVarName, profilesString);
        restart(appId);
    }

    private void restart(String appId) {
        this.cloudFoundryOperations.applications()
                .restart(RestartApplicationRequest.builder().name(appId).build()).block();
    }

    private String deployEurekaService() throws Throwable {
        return this.service
                .applicationManifestFrom(this.eurekaManifest)
                .entrySet()
                .stream()
                .map(e -> {
                    String appId = e.getValue().getName();
                    if (!this.service.applicationExists(appId))
                        service.pushApplicationAndCreateUserDefinedServiceUsingManifest(e.getKey(), e.getValue());
                    return appId;
                })
                .findAny()
                .orElse(null);
    }

    private static String[] profiles(String... profiles) {
        Collection<String> p = new ArrayList<>();
        if (null != profiles && 0 != profiles.length) {
            p.addAll(Arrays.asList(profiles));
        }
        p.add("cloud");
        return p.toArray(new String[p.size()]);
    }

    private String deployGreetingsService()
            throws Throwable {
        return this
                .service
                .applicationManifestFrom(this.greetingsServiceManifest)
                .entrySet()
                .stream()
                .map(e -> {
                    File f = e.getKey();
                    ApplicationManifest am = e.getValue();
                    String appId = am.getName();
                    if (!this.service.applicationExists(appId))
                        this.service.pushApplicationUsingManifest(f, am, true);
                    return appId;
                })
                .findAny()
                .orElse(null);
    }

    @Before
    public void before() throws Throwable {
        File root = new File(".");
        this.eurekaManifest = new File(root, "../service-registry/manifest.yml");
        this.edgeServiceManifest = new File(root, "../edge-service/manifest.yml");
        this.greetingsServiceManifest = new File(root,
                "../greetings-service/manifest.yml");
        Assert.assertTrue(this.greetingsServiceManifest.exists());
        Assert.assertTrue(this.eurekaManifest.exists());
        Assert.assertTrue(this.edgeServiceManifest.exists());

        // todo destroy the existing apps

    }

    @Test
    public void restClients() throws Throwable {
        String[] ps = {"insecure"};
        baselineDeploy(ps);
        testEdgeRestClient("Shafer", "/api/resttemplate/");
    }

    private void testEdgeRestClient(String testName, String urlSuffix) {
        String root = this.service.urlForApplication("edge-service");
        String edgeServiceUrl = root + urlSuffix + testName;
        String healthUrl = root + "/health" ;
        ResponseEntity<String> responseEntity = this.rt.getForEntity(healthUrl, String.class);
        log.info("health endpoint: "+ responseEntity.getBody());

        ResponseEntity<String> response = this.rt.getForEntity(edgeServiceUrl, String.class);
        String body = response.getBody();
        Assert.assertTrue(body.contains("Hello, " + testName));
    }

    @Test
    public void testFeignClients() throws Throwable {
        String[] ps = {"insecure", "feign"};
        baselineDeploy(ps);
        testEdgeRestClient("Watters", "/api/feign/");
    }

    private void baselineDeploy(String[] ps) throws Throwable {
        String eurekaServiceId = deployEurekaService();
        this.log.info("deployed " + eurekaServiceId);
        String greetingsServiceId = this.deployGreetingsService();
        this.reconfigureApplicationProfile(greetingsServiceId, new String[]{"insecure"});
        this.setEnvironmentVariable(greetingsServiceId, "security.basic.enabled", "false");
        this.restart(greetingsServiceId);
        this.log.info("deployed " + greetingsServiceId);

        String edgeServiceId = this.deployEdgeService();
        this.reconfigureApplicationProfile(edgeServiceId, ps);
        this.setEnvironmentVariable(edgeServiceId, "security.basic.enabled", "false");
        this.restart(edgeServiceId);
        this.log.info("deployed " + edgeServiceId);
    }

    private String deployEdgeService() {
        return this.service.applicationManifestFrom(this.edgeServiceManifest)
                .entrySet()
                .stream()
                .map(e -> {
                    File f = e.getKey();
                    ApplicationManifest am = e.getValue();
                    String appId = am.getName();
                    if (!this.service.applicationExists(appId))
                        this.service.pushApplicationUsingManifest(f, am, false);
                    return appId;
                })
                .findAny()
                .orElse(null);

    }
}
