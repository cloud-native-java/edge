package greetings;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AsciiString;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.config.HttpClientOptions;
import reactor.ipc.netty.http.HttpClient;
import reactor.ipc.netty.http.HttpClientRequest;
import reactor.ipc.netty.http.HttpClientResponse;
import reactor.ipc.netty.http.HttpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@EnableAutoConfiguration
@Configuration
public class ReactorGreetingsClientApiGateway {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .registerModule(new Jdk8Module())
                .setSerializationInclusion(NON_NULL);
    }

    @Bean
    CommandLineRunner client(ReactorOperations reactorOperations) {
        return args -> {

            Mono<String> mono = reactorOperations.doGet(String.class, builder -> builder, httpClientRequest -> httpClientRequest);
            System.out.println(mono.map(s -> s).block());

        };

    }

    @Bean
    ReactorOperations reactorOperations(HttpClient httpClient, ObjectMapper objectMapper) {
        return new ReactorOperations(httpClient, objectMapper, Mono.just("http://google.com"));
    }

    @Bean
    HttpClient httpClient() {
        return HttpClient.create(HttpClientOptions.create().sslSupport());
    }

    public static void main(String args[]) throws Exception {
        SpringApplication.run(ReactorGreetingsClientApiGateway.class);
    }
}
/*
@Profile("reactor")
@RestController
@RequestMapping("/api")
class ReactorGreetingsClientApiGateway {

    private final RestTemplate restTemplate;

    @Autowired
    ReactorGreetingsClientApiGateway(@LoadBalanced RestTemplate restTemplate) { // <1>
        this.restTemplate = restTemplate;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/resttemplate/{name}")
    Map<String, String> restTemplate(@PathVariable String name) {

        ParameterizedTypeReference<Map<String, String>> type =
                new ParameterizedTypeReference<Map<String, String>>() {
                };

        return this.restTemplate.exchange(
                "http://greetings-service/greet/{name}",
                HttpMethod.GET, null, type, name).getBody();
    }
}*/

class NetworkLogging {

    public static final Logger REQUEST_LOGGER = LoggerFactory.getLogger("cloudfoundry-client.request");

    public static final Logger RESPONSE_LOGGER = LoggerFactory.getLogger("cloudfoundry-client.response");

    private static final String CF_WARNINGS = "X-Cf-Warnings";

    public static Consumer<Subscription> delete(String uri) {
        return s -> REQUEST_LOGGER.debug("DELETE {}", uri);
    }

    public static Consumer<Subscription> get(String uri) {
        return s -> REQUEST_LOGGER.debug("GET    {}", uri);
    }

    public static Consumer<Subscription> patch(String uri) {
        return s -> REQUEST_LOGGER.debug("PATCH  {}", uri);
    }

    public static Consumer<Subscription> post(String uri) {
        return s -> REQUEST_LOGGER.debug("POST   {}", uri);
    }

    public static Consumer<Subscription> put(String uri) {
        return s -> REQUEST_LOGGER.debug("PUT    {}", uri);
    }

    public static Function<Mono<HttpClientResponse>, Mono<HttpClientResponse>> response(String uri) {
        return inbound -> inbound
                .doOnSuccess(i -> {
                    List<String> warnings = i.responseHeaders().getAll(CF_WARNINGS);

                    if (warnings.isEmpty()) {
                        RESPONSE_LOGGER.debug("{}    {}", i.status().code(), uri);
                    } else {
                        RESPONSE_LOGGER.warn("{}    {} ({})", i.status().code(), uri, StringUtils.collectionToCommaDelimitedString(warnings));
                    }
                })
                .doOnError(t -> {
                    if (t instanceof HttpException) {
                        RESPONSE_LOGGER.debug("{}    {}", ((HttpException) t).getResponseStatus().code(), uri);
                    }
                });
    }

    public static Consumer<Subscription> ws(String uri) {
        return s -> REQUEST_LOGGER.debug("WS     {}", uri);
    }

}

class ReactorOperations {

    protected static final AsciiString APPLICATION_JSON = new AsciiString("application/json");
    protected static final AsciiString APPLICATION_X_WWW_FORM_URLENCODED = new AsciiString("application/x-www-form-urlencoded");
    protected static final AsciiString APPLICATION_ZIP = new AsciiString("application/zip");
    protected static final AsciiString AUTHORIZATION = new AsciiString("Authorization");
    protected static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");

    private final ObjectMapper objectMapper;
    private final Mono<String> root;
    private final HttpClient httpClient;

    protected ReactorOperations(HttpClient httpClient,
                                ObjectMapper objectMapper,
                                Mono<String> root) {
        this.root = root;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    protected final <T> Mono<T> doDelete(Object request, Class<T> responseType, Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer,
                                         Function<HttpClientRequest, HttpClientRequest> requestTransformer) {
        return this.root
                .map(root -> buildUri(root, uriTransformer))
                .then(uri -> this.httpClient
                        .delete(uri, outbound -> Mono.just(outbound)
                                .map(requestTransformer)
                                .then(o -> o.send(serializedRequest(o, request))))
                        .doOnSubscribe(NetworkLogging.delete(uri))
                        .compose(NetworkLogging.response(uri)))
                .compose(deserializedResponse(responseType));
    }

    protected final <T> Mono<T> doGet(Class<T> responseType, Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer,
                                      Function<HttpClientRequest, HttpClientRequest> requestTransformer) {
        return doGet(uriTransformer, requestTransformer)
                .compose(deserializedResponse(responseType));
    }

    protected final Mono<HttpClientResponse> doGet(Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer, Function<HttpClientRequest, HttpClientRequest> requestTransformer) {
        return this.root
                .map(root -> buildUri(root, uriTransformer))
                .then(uri -> this.httpClient
                        .get(uri, outbound -> Mono.just(outbound)
                                .map(requestTransformer)
                                .then(HttpClientRequest::sendHeaders))
                        .doOnSubscribe(NetworkLogging.get(uri))
                        .compose(NetworkLogging.response(uri)));
    }

    protected final <T> Mono<T> doPatch(Object request, Class<T> responseType, Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer,
                                        Function<HttpClientRequest, HttpClientRequest> requestTransformer) {
        return this.root
                .map(root -> buildUri(root, uriTransformer))
                .then(uri -> this.httpClient
                        .patch(uri, outbound -> Mono.just(outbound)
                                .map(requestTransformer)
                                .then(o -> o.send(serializedRequest(o, request))))
                        .doOnSubscribe(NetworkLogging.patch(uri))
                        .compose(NetworkLogging.response(uri)))
                .compose(deserializedResponse(responseType));
    }

    protected final <T> Mono<T> doPost(Object request, Class<T> responseType, Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer,
                                       Function<HttpClientRequest, HttpClientRequest> requestTransformer) {

        return doPost(responseType, uriTransformer, outbound -> requestTransformer.apply(outbound)
                .send(serializedRequest(outbound, request)));
    }

    protected final <T> Mono<T> doPost(Class<T> responseType, Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer, Function<HttpClientRequest, Mono<Void>> requestTransformer) {
        return this.root
                .map(root -> buildUri(root, uriTransformer))
                .then(uri -> this.httpClient
                        .post(uri, outbound -> Mono.just(outbound)
                                .then(requestTransformer))
                        .doOnSubscribe(NetworkLogging.post(uri))
                        .compose(NetworkLogging.response(uri)))
                .compose(deserializedResponse(responseType));
    }

    protected final <T> Mono<T> doPut(Object request, Class<T> responseType, Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer,
                                      Function<HttpClientRequest, HttpClientRequest> requestTransformer) {
        return this.root
                .map(root -> buildUri(root, uriTransformer))
                .then(uri -> this.httpClient
                        .put(uri, outbound -> Mono.just(outbound)
                                .map(requestTransformer)
                                .then(o -> o.send(serializedRequest(o, request))))
                        .doOnSubscribe(NetworkLogging.put(uri))
                        .compose(NetworkLogging.response(uri)))
                .compose(deserializedResponse(responseType));
    }

    protected final <T> Mono<T> doPut(Class<T> responseType, Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer, Function<HttpClientRequest, Mono<Void>> requestTransformer) {
        return this.root
                .map(root -> buildUri(root, uriTransformer))
                .then(uri -> this.httpClient
                        .put(uri, outbound -> Mono.just(outbound)
                                .then(requestTransformer))
                        .doOnSubscribe(NetworkLogging.put(uri))
                        .compose(NetworkLogging.response(uri)))
                .compose(deserializedResponse(responseType));
    }

    protected final Mono<HttpClientResponse> doWs(Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer, Function<HttpClientRequest, HttpClientRequest> requestTransformer) {
        return this.root
                .map(root -> buildUri(root, uriTransformer))
                .then(uri -> this.httpClient
                        .get(uri, outbound -> Mono.just(outbound)
                                .map(requestTransformer)
                                .then(HttpClientRequest::upgradeToTextWebsocket))
                        .doOnSubscribe(NetworkLogging.ws(uri))
                        .compose(NetworkLogging.response(uri)));
    }

    private static String buildUri(String root, Function<UriComponentsBuilder, UriComponentsBuilder> uriTransformer) {
        return uriTransformer
                .apply(UriComponentsBuilder.fromUriString(root))
                .build().encode().toUriString();
    }

    private <T> Function<Mono<HttpClientResponse>, Mono<T>> deserializedResponse(Class<T> responseType) {
        return inbound -> inbound
                .then(i -> i.receive().aggregate().toInputStream())
                .map(JsonCodec.decode(this.objectMapper, responseType))
                .doOnError(JsonParsingException.class, e -> NetworkLogging.RESPONSE_LOGGER.debug("\n{}", e.getPayload()));
    }

    private Mono<ByteBuf> serializedRequest(HttpClientRequest outbound, Object request) {
        return Mono.just(request)
                .filter(req -> this.objectMapper.canSerialize(req.getClass()))
                .map(JsonCodec.encode(this.objectMapper, outbound));
    }

}

class JsonParsingException extends RuntimeException {

    private static final long serialVersionUID = 689280281752742553L;

    private final String payload;

    JsonParsingException(String message, Throwable cause, InputStream in) {
        super(message, cause);
        this.payload = getPayload(in);
    }

    public String getPayload() {
        return this.payload;
    }

    private static String getPayload(InputStream in) {
        StringBuilder sb = new StringBuilder();

        try {
            in.reset();

            try (Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"))) {

                int length;
                char[] buffer = new char[8192];
                while ((length = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }

        return sb.toString();
    }

}

class JsonCodec {

    private static final AsciiString APPLICATION_JSON = new AsciiString("application/json; charset=utf-8");

    private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");

    public static <T> Function<InputStream, T> decode(ObjectMapper objectMapper, Class<T> type) {
        return inputStream -> {
            try (InputStream in = inputStream) {
                return objectMapper.readValue(in, type);
            } catch (IOException e) {
                throw new JsonParsingException("Unable to parse JSON Payload", e, inputStream);
            }
        };
    }

    static <T> Function<T, ByteBuf> encode(ObjectMapper objectMapper, HttpClientRequest request) {
        request.header(CONTENT_TYPE, APPLICATION_JSON);
        return source -> encode(request.delegate().alloc(), objectMapper, source);
    }

    static <T> ByteBuf encode(ByteBufAllocator allocator, ObjectMapper objectMapper, T source) {
        try {
            return allocator.directBuffer().writeBytes(objectMapper.writeValueAsBytes(source));
        } catch (JsonProcessingException e) {
            throw Exceptions.propagate(e);
        }
    }

}


class XReactorGreetingsClient {
//    public HttpClient getHttpClient() {
////        ClientOptions options = HttpClientOptions.create()
////                .sslSupport()
////                .sndbuf(SEND_BUFFER_SIZE)
////                .rcvbuf(RECEIVE_BUFFER_SIZE);
////
////        getKeepAlive().ifPresent(options::keepAlive);
////        getProxyConfiguration().ifPresent(c -> options.proxy(ClientOptions.Proxy.HTTP, c.getHost(), c.getPort().orElse(null), c.getUsername().orElse(null), u -> c.getPassword().orElse(null)));
////        getSocketTimeout().ifPresent(options::timeout);
////        getSslCertificateTruster().ifPresent(trustManager -> options.ssl().trustManager(new StaticTrustManagerFactory(trustManager)));
////        getSslHandshakeTimeout().ifPresent(options::sslHandshakeTimeout);
////
////        return HttpClient.create(options);
//    }
//
}
