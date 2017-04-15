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
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

@Ignore
public abstract class AbstractEdgeTest {

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected RetryTemplate retryTemplate;

    @Autowired
    protected CloudFoundryOperations cloudFoundryOperations;

    @Autowired
    protected CloudFoundryService service;

    protected File root, authServiceManifest, eurekaManifest, edgeServiceManifest, greetingsServiceManifest, html5ClientManifest;

    private static volatile boolean RESET = false;

    private Log log = LogFactory.getLog(getClass());

    @Before
    public void before() throws Throwable {
        log.info("RESET=" + RESET);
        baseline(RESET);
        RESET = false;
    }


    protected void destroy() throws Throwable {
        log.info("destroy()");
        String authServiceAppId = this.appNameFromManifest(this.authServiceManifest);
        String eurekaAppId = this.appNameFromManifest(this.eurekaManifest);
        String html5AppId = this.appNameFromManifest(this.html5ClientManifest);
        String edgeServiceAppId = this.appNameFromManifest(this.edgeServiceManifest);
        String greetingsServiceAppId = this.appNameFromManifest(this.greetingsServiceManifest);
        Stream.of(html5AppId, edgeServiceAppId, greetingsServiceAppId, eurekaAppId,
                authServiceAppId).forEach(appId -> {
            try {
                this.service.destroyApplicationIfExists(appId);
                this.log.info("attempted to delete application " + appId);
            } catch (Throwable t) {
                // don't care
            }
        });

        Stream.of(eurekaAppId, authServiceAppId).forEach(svcId -> {
            try {
                this.service.destroyServiceIfExists(svcId);
                log.info("attempted to delete service " + svcId);
            } catch (Throwable t) {
                // don't care
            }
        });
    }

    protected void setEnvironmentVariable(String appId, String k, String v) {
        log.info("set-env " + appId + " " + k + " " + v);
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
        this.setEnvironmentVariable(appId, profileVarName, profilesString);
    }

    protected void restart(String appId) {
        this.cloudFoundryOperations
                .applications()
                .restart(RestartApplicationRequest.builder().name(appId).build()).block();
        log.info("restarted " + appId);
    }

    protected static String[] profiles(String... profiles) {
        Collection<String> p = new ArrayList<>();
        if (null != profiles && 0 != profiles.length) {
            p.addAll(Arrays.asList(profiles));
        }
        p.add("cloud");
        return p.toArray(new String[p.size()]);
    }

    private String deployAppAndServiceIfDoesNotExist(File manifest) {

        String appName = this.appNameFromManifest(manifest);
        this.log.info("deploying " + appName);
        this.service
                .applicationManifestFrom(manifest)
                .entrySet()
                .stream()
                .map(e -> {
                    if (!service.applicationExists(appName)) {
                        //  service.destroyServiceIfExists(appId);
                        service.pushApplicationAndCreateUserDefinedServiceUsingManifest(
                                e.getKey(), e.getValue());
                        this.log.info("deployed " + appName + ".");
                    }
                    return appName;
                })
                .findAny()
                .orElse(null);
        return appName;
    }

    protected String deployAppIfDoesNotExist(File manifest) {
        String appName = this.appNameFromManifest(manifest);
        this.log.info("deploying " + appName);

        this
                .service
                .applicationManifestFrom(manifest)
                .entrySet()
                .stream()
                .map(e -> {
                    File f = e.getKey();
                    ApplicationManifest am = e.getValue();
                    String appId = am.getName();
                    if (!this.service.applicationExists(appId)) {
                        this.service.pushApplicationUsingManifest(f, am, false);
                        this.log.info("deployed " + appName + ".");
                    }
                    return appId;
                })
                .findAny()
                .orElse(null);

        return appName;
    }

    public void baseline(boolean delete) throws Throwable {
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

        if (delete) {
            this.destroy();
        }
    }

    protected String appNameFromManifest(File a) {
        return this
                .service
                .applicationManifestFrom(a)
                .entrySet()
                .stream()
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

        this.deployAppAndServiceIfDoesNotExist(this.eurekaManifest);
        this.deployAppAndServiceIfDoesNotExist(this.authServiceManifest);
        deployAppWithSettings(this.greetingsServiceManifest, gsProfiles, gsEnv, gsCallback);
        deployAppWithSettings(this.edgeServiceManifest, esProfiles, esEnv, esCallback);
    }

    private void deployAppWithSettings(File ma, String[] profiles, Map<String, String> env, ApplicationInstanceConfiguration callback) {
        String appId = this.deployAppIfDoesNotExist(ma);
        if (null != callback) {
            callback.configure(appId);
        }
        this.reconfigureApplicationProfile(appId, profiles);
        env.forEach((k, v) -> this.setEnvironmentVariable(appId, k, v));
        this.restart(appId);
    }

}
