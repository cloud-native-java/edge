package edge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.parseMediaType;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class CorsIT extends AbstractEdgeTest {

 private Log log = LogFactory.getLog(getClass());

 private String edgeServiceAppId;

 @Before
 public void before() throws Throwable {
  this.defaultSetup(true);
  this.edgeServiceAppId = this.appNameFromManifest(this.edgeServiceManifest);
 }

 @Ignore
 @Test
 public void testCors() throws Throwable {
  Map<String, String> e = Collections.singletonMap("security.basic.enabled",
   "false");
  this.baselineDeploy(new String[] { "insecure" }, e, null,
   "cors,insecure".split(","), e, null);
  this.deployHtml5Client();

  /*
   * we'll do a CORS preflight and
   * confirm that the edge-service gives
   * us an A..C..-Allow-Origin response.
   * This only works if we ask it to
   * allot access for a service about
   * which it knows. Which means we can
   * only ask for a service that's been
   * registered in the registry
   */
  String edgeServiceUri = service.urlForApplication(this.edgeServiceAppId)
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

  ResponseEntity<Void> responseEntity = restTemplate.exchange(requestEntity,
   Void.class);
  HttpHeaders headers = responseEntity.getHeaders();
  headers.forEach((k, v) -> log.info(k + '=' + v.toString()));
  log.info("response received: " + responseEntity.toString());
  Assert.assertTrue("our preflight response should contain a "
   + ACCESS_CONTROL_ALLOW_ORIGIN,
   headers.containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
 }

}
