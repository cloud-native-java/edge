package client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@EnableDiscoveryClient
@SpringBootApplication
public class Html5Client {

 private final LoadBalancerClient loadBalancerClient;

 @Autowired
 Html5Client(LoadBalancerClient loadBalancerClient) {
  this.loadBalancerClient = loadBalancerClient;
 }

 public static void main(String[] args) {
  SpringApplication.run(Html5Client.class, args);
 }

 // <1>
 @GetMapping(value = "/greetings-client-uri", produces = MediaType.APPLICATION_JSON_VALUE)
 Map<String, String> greetingsClientURI() throws Exception {
  return Optional
   .ofNullable(this.loadBalancerClient.choose("greetings-client"))
   .map(si -> Collections.singletonMap("uri", si.getUri().toString()))
   .orElse(null);
 }
}
