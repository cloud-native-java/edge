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
import org.springframework.http.*;
import org.springframework.retry.RetryCallback;
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

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.parseMediaType;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class EdgeIT {

 @Autowired
 private RestTemplate restTemplate;

 @Autowired
 private RetryTemplate retryTemplate;

 @Autowired
 private CloudFoundryOperations cloudFoundryOperations;

 @Autowired
 private CloudFoundryService service;

 private File root, authServiceManifest, eurekaManifest, edgeServiceManifest,
  greetingsServiceManifest, html5ClientManifest;

 // never deletes the apps if theyre
 // already there
 private static volatile boolean RESET = false;

 private Log log = LogFactory.getLog(getClass());

 @Before
 public void before() throws Throwable {
  log.info("RESET=" + RESET);
  baseline(RESET);
  RESET = false;
 }

 @Test
 public void restClients() throws Throwable {

  log.info("running restClients()");
  // resttemplate
  baselineDeploy(new String[] { "insecure" },
   Collections.singletonMap("security.basic.enabled", "false"), null,
   new String[] { "insecure" },
   Collections.singletonMap("security.basic.enabled", "false"), null);
  testEdgeRestClient("Shafer", "/api/resttemplate/");

  // feign
  baselineDeploy(new String[] { "insecure" },
   Collections.singletonMap("security.basic.enabled", "false"), null,
   "insecure,feign".split(","),
   Collections.singletonMap("security.basic.enabled", "false"), null);
  testEdgeRestClient("Watters", "/api/feign/");
 }

 //@Test
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

  baselineDeploy(new String[] { "secure" }, new HashMap<>(), callback,
   new String[] { "secure", "sso" }, new HashMap<>(), callback);

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
  this.baselineDeploy(new String[] { "insecure" }, e, null,
   "cors,insecure".split(","), e, null);
  String edgeServiceUri = this.service
   .urlForApplication(appNameFromManifest(this.edgeServiceManifest))
   + "/lets/greet/Phil";
  String html5ClientUri = this.service.urlForApplication(this
   .appNameFromManifest(this.html5ClientManifest));
  this.log.info("edge-service URI " + edgeServiceUri);
  this.log.info("html5-client URI " + html5ClientUri);
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
   if (!exchange.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)) {
    log.info(ACCESS_CONTROL_ALLOW_ORIGIN + " not present in response.");
    throw new RuntimeException("there's no " + ACCESS_CONTROL_ALLOW_ORIGIN
     + " header present.");
   }
   return exchange;
  });
  HttpHeaders headers = responseEntity.getHeaders();
  headers.forEach((k, v) -> log.info(k + '=' + v.toString()));
  log.info("response received: " + responseEntity.toString());
  Assert.assertTrue("preflight response should contain "
   + ACCESS_CONTROL_ALLOW_ORIGIN,
   headers.containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
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

 private void destroy() throws Throwable {
  log.info("destroy()");
  String authServiceAppId = this.appNameFromManifest(this.authServiceManifest);
  String eurekaAppId = this.appNameFromManifest(this.eurekaManifest);
  String html5AppId = this.appNameFromManifest(this.html5ClientManifest);
  String edgeServiceAppId = this.appNameFromManifest(this.edgeServiceManifest);
  String greetingsServiceAppId = this
   .appNameFromManifest(this.greetingsServiceManifest);
  Stream.of(html5AppId, edgeServiceAppId, greetingsServiceAppId, eurekaAppId,
   authServiceAppId).forEach(appId -> {
   try {
    this.service.destroyApplicationIfExists(appId);
    this.log.info("attempted to delete application " + appId);
   }
   catch (Throwable t) {
    // don't care
  }
 });

  Stream.of(eurekaAppId, authServiceAppId).forEach(svcId -> {
   try {
    this.service.destroyServiceIfExists(svcId);
    log.info("attempted to delete service " + svcId);
   }
   catch (Throwable t) {
    // don't care
  }
 });
 }

 private void setEnvironmentVariable(String appId, String k, String v) {
  log.info("set-env " + appId + " " + k + " " + v);
  this.cloudFoundryOperations
   .applications()
   .setEnvironmentVariable(
    SetEnvironmentVariableApplicationRequest.builder().name(appId)
     .variableName(k).variableValue(v).build()).block();
 }

 private void reconfigureApplicationProfile(String appId, String profiles[]) {
  String profileVarName = "spring_profiles_active".toUpperCase();
  String profilesString = StringUtils
   .arrayToCommaDelimitedString(profiles(profiles));
  this.setEnvironmentVariable(appId, profileVarName, profilesString);
 }

 private void restart(String appId) {
  this.cloudFoundryOperations.applications()
   .restart(RestartApplicationRequest.builder().name(appId).build()).block();
  log.info("restarted " + appId);
 }

 private static String[] profiles(String... profiles) {
  Collection<String> p = new ArrayList<>();
  if (null != profiles && 0 != profiles.length) {
   p.addAll(Arrays.asList(profiles));
  }
  p.add("cloud");
  return p.toArray(new String[p.size()]);
 }

 private void deployAppAndServiceIfDoesNotExist(File manifest) {

  String appName = this.appNameFromManifest(manifest);
  this.log.info("deploying " + appName);
  this.service
   .applicationManifestFrom(manifest)
   .entrySet()
   .stream()
   .map(e -> {
    if (!service.applicationExists(appName)) {
     // service.destroyServiceIfExists(appId);
    service.pushApplicationAndCreateUserDefinedServiceUsingManifest(e.getKey(),
     e.getValue());
    this.log.info("deployed " + appName + ".");
   }
   return appName;
  }).findAny().orElse(null);
 }

 private String deployAppIfDoesNotExist(File manifest) {
  String appName = this.appNameFromManifest(manifest);
  this.log.info("deploying " + appName);

  this.service.applicationManifestFrom(manifest).entrySet().stream().map(e -> {
   File f = e.getKey();
   ApplicationManifest am = e.getValue();
   String appId = am.getName();
   if (!this.service.applicationExists(appId)) {
    this.service.pushApplicationUsingManifest(f, am, false);
    this.log.info("deployed " + appName + ".");
   }
   return appId;
  }).findAny().orElse(null);

  return appName;
 }

 private void baseline(boolean delete) throws Throwable {
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

 private String appNameFromManifest(File a) {
  return this.service.applicationManifestFrom(a).entrySet().stream()
   .map(e -> e.getValue().getName()).findAny().orElse(null);
 }

 public interface ApplicationInstanceConfiguration {

  void configure(String appId);
 }

 private void baselineDeploy(

            // greetings-service
            String[] gsProfiles, Map<String, String> gsEnv,
            ApplicationInstanceConfiguration gsCallback,

            // edge-service
            String[] esProfiles, Map<String, String> esEnv,
            ApplicationInstanceConfiguration esCallback

    ) throws Throwable {

        // backing services
        this.deployBackingServices();

        this.deployAppAndServiceIfDoesNotExist(this.eurekaManifest);
        this.deployAppAndServiceIfDoesNotExist(this.authServiceManifest);
        this.deployAppWithSettings(this.greetingsServiceManifest, gsProfiles, gsEnv,
                gsCallback);
        this.deployAppWithSettings(this.edgeServiceManifest, esProfiles, esEnv,
                esCallback);
        this.deployAppIfDoesNotExist(this.html5ClientManifest);

    }

 private void deployBackingServices() {
  service.createServiceIfMissing("elephantsql", "turtle", "auth-service-pgsql");
 }

 private void deployAppWithSettings(File ma, String[] profiles,
  Map<String, String> env, ApplicationInstanceConfiguration callback) {
  String appId = this.deployAppIfDoesNotExist(ma);
  if (null != callback) {
   callback.configure(appId);
  }
  this.reconfigureApplicationProfile(appId, profiles);
  env.forEach((k, v) -> this.setEnvironmentVariable(appId, k, v));
  this.restart(appId);
 }

}
