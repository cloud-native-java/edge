package edge;

import auth.AuthServiceApplication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AuthServiceApplicationTests {

    private static AtomicInteger PORT = new AtomicInteger();

    @Configuration
    @Import(AuthServiceApplication.class)
    public static class AuthConfig {

        @EventListener(EmbeddedServletContainerInitializedEvent.class)
        public void ready(EmbeddedServletContainerInitializedEvent evt) {
            PORT.set(evt.getEmbeddedServletContainer().getPort());
        }
    }

    private ApplicationContext applicationContext;
    private final Log log = LogFactory.getLog(getClass());
    private RestTemplate restTemplate;
    private int port = 0;

    @Before
    public void setUp() throws Exception {
        this.restTemplate = new RestTemplate();
        this.applicationContext = SpringApplication.run(AuthConfig.class);
        this.port = PORT.get();
    }

    @Test
    public void generateToken() throws Exception {
        URI uri = URI.create("http://localhost:" + this.port + "/uaa/oauth/token");
        String username = "jlong";
        String password = "spring";
        String clientSecret = "acmesecret";
        String client = "acme";
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>() {
            {
                add("client_secret", clientSecret);
                add("client_id", client);
                add("scope", "openid");
                add("grant_type", "password");
                add("username", username);
                add("password", password);
            }
        };

        String token = Base64Utils.encodeToString((client + ":" + clientSecret).getBytes(Charset.forName("UTF-8")));

        RequestEntity<LinkedMultiValueMap<String, String>> requestEntity =
                RequestEntity
                        .post(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic " + token)
                        .body(map);

        ParameterizedTypeReference<Map<String, String>> type =
                new ParameterizedTypeReference<Map<String, String>>() {};

        ResponseEntity<Map<String, String>> responseEntity = this.restTemplate.exchange(requestEntity, type);

        Map<String, String> body = responseEntity.getBody();

        log.info("access_token: " + body.get("access_token"));
    }

}
