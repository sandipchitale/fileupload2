package sandipchitale.fileupload2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

@RestController
public class WebClientUploadController {
    private final WebClient.Builder webClientBuilder;

    public WebClientUploadController() {
        webClientBuilder = WebClient.builder();
    }

    @PostMapping("/upload")
    public ResponseEntity<byte[]> upload(HttpServletRequest httpServletRequest,
                                         @RequestHeader MultiValueMap<String, String> requestHeaders) throws IOException {
        if (httpServletRequest.getContentType().toLowerCase().startsWith("multipart/form-data")) {
            byte[] responseBytes = webClientBuilder
                    .build()
                    .post()
                    .uri("http://localhost:9000/receive")
                    .headers((HttpHeaders headers) -> headers.addAll(requestHeaders))
                    .body(BodyInserters.fromResource(new InputStreamResource(httpServletRequest.getInputStream())))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            return ResponseEntity.ok().body(responseBytes);
        }
        return ResponseEntity.ok().body("Not a multipart/form-data request".getBytes());
    }

}