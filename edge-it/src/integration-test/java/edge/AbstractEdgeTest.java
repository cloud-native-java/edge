package edge;

import cnj.CloudFoundryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractEdgeTest {

 @Autowired
 protected CloudFoundryOperations cloudFoundryOperations;

 @Autowired
 protected CloudFoundryService service;

 protected File root, authServiceManifest, eurekaManifest, edgeServiceManifest,
  greetingsServiceManifest, html5ClientManifest;

 private Log log = LogFactory.getLog(getClass());

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
}
