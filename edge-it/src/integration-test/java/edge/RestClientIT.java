package edge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class RestClientIT extends AbstractEdgeTest {

    private RetryTemplate retryTemplate = new RetryTemplate();
    private final RestTemplate restTemplate = new RestTemplate();
    private Log log = LogFactory.getLog(getClass());

    @Before
    public void before() throws Throwable {
        this.defaultSetup();
    }

    private void testEdgeRestClient(String testName, String urlSuffix) throws Throwable {
        String root = this.service.urlForApplication("edge-service");
        String edgeServiceUrl = root + urlSuffix + testName;
        String healthUrl = root + "/health";
        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(healthUrl, String.class);
        log.info("health endpoint: " + responseEntity.getBody());
        String body = retryTemplate.execute((RetryCallback<String, Throwable>) context -> {
            ResponseEntity<String> response = restTemplate.getForEntity(edgeServiceUrl, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                String msg = "couldn't get a valid response calling the edge service ";
                this.log.info(msg);
                throw new RuntimeException(msg + edgeServiceUrl);
            }
            return response.getBody();
        });
        Assert.assertTrue(body.contains("Hello, " + testName));
    }

    @Test
    public void restClients() throws Throwable {
        String[] ps = {"insecure"};
        baselineDeploy(ps);
        testEdgeRestClient("Shafer", "/api/resttemplate/");
    }

    @Test
    public void testFeignClients() throws Throwable {
        String[] ps = {"insecure", "feign"};
        baselineDeploy(ps);
        testEdgeRestClient("Watters", "/api/feign/");
    }

}
