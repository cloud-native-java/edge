package edge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.applications.UnsetEnvironmentVariableApplicationRequest;
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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class PasswordGrantIT extends AbstractEdgeTest {

 private Log log = LogFactory.getLog(getClass());

 @Autowired
 private RestTemplate restTemplate;

 @Autowired
 private RetryTemplate retryTemplate;

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
}
