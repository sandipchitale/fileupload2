package sandipchitale.fileupload2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientRequest;

import java.io.IOException;
import java.time.Duration;

@RestController
public class WebClientUploadController {
    private final WebClient.Builder webClientBuilder;

    public WebClientUploadController() {
        this.webClientBuilder = WebClient.builder()
                .baseUrl("http://localhost:9000")
                .filter(getXTimeoutMillisFilter())
                .filter(getXFrameOptionsFilter());
    }

    /**
     * If X-Timeout-Millis request header is present in the request then set the response timeout to that value.
     * Remove cookie header.
     *
     * @return
     */
    private static ExchangeFilterFunction getXTimeoutMillisFilter() {
        return ExchangeFilterFunction.ofRequestProcessor((ClientRequest clientRequest) -> {
            String timeoutMillis = clientRequest.headers().getFirst("X-Timeout-Millis");
            if (timeoutMillis != null) {
                return Mono.just(ClientRequest.from(clientRequest)
                        .headers(httpHeaders -> {
                            // Cleanup the header
                            // httpHeaders.remove("X-Timeout-Millis");
                            // sensitive headers
                            httpHeaders.remove(HttpHeaders.COOKIE);
                        })
                        .httpRequest((ClientHttpRequest clientHttpRequest) -> {
                            HttpClientRequest reactorRequest = clientHttpRequest.getNativeRequest();
                            reactorRequest.responseTimeout(Duration.ofMillis(Long.parseLong(timeoutMillis)));
                        })
                        .build());
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Set X-Frame-Options response header to SAMEORIGIN.
     *
     * @return
     */
    private static ExchangeFilterFunction getXFrameOptionsFilter() {
        return ExchangeFilterFunction.ofResponseProcessor((ClientResponse clientResponse) -> {
            clientResponse = clientResponse
                    .mutate()
                    .headers(httpHeaders -> {
                        // Remove some headers
                        // httpHeaders.remove(HttpHeaders.SET_COOKIE);
                    })
                    .header("X-Frame-Options", "SAMEORIGIN")
                    .build();
            return Mono.just(clientResponse);
        });
    }

    @PostMapping("/upload")
    public ResponseEntity<byte[]> upload(HttpServletRequest httpServletRequest,
                                         @RequestHeader HttpHeaders requestHeaders) throws IOException {
        if (httpServletRequest.getContentType().toLowerCase().startsWith("multipart/form-data")) {
            return webClientBuilder
                    .build()
                    .post()
                    .uri("/receive")
                    .headers((HttpHeaders headers) -> headers.addAll(requestHeaders))
                    // Pass thru body as is modulo header processing in
                    // request processor ExchangeFilterFunction above
                    .body(BodyInserters.fromResource(new InputStreamResource(httpServletRequest.getInputStream())))
                    .retrieve()
                    // return response as is modulo header processing in
                    // response processor ExchangeFilterFunction above
                    .toEntity(byte[].class)
                    .block();
        }
        return ResponseEntity.ok().body("Not a multipart/form-data request".getBytes());
    }

}