package sandipchitale.fileupload2;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.core.FileItemHeaders;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

@SpringBootApplication
public class Fileupload2Application {
	
	@RestController
	public static class UploadController {
		private final RestTemplate restTemplate;

		public UploadController(RestTemplateBuilder restTemplateBuilder) {
			this.restTemplate = restTemplateBuilder
					.setBufferRequestBody(false)
					.build();
			restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
			restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter() {
				@Override
				public boolean supports(Class<?> clazz) {
					return true;
				}
			});
		}

		private static class NoOpResponseErrorHandler extends DefaultResponseErrorHandler {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		}

		@PostMapping("/upload")
	    public String upload(HttpServletRequest httpServletRequest,
							 @RequestHeader MultiValueMap<String, String> requestHeaders) {
			boolean multipartContent = JakartaServletFileUpload.isMultipartContent(httpServletRequest);
			if (multipartContent) {
				JakartaServletFileUpload jakartaServletFileUpload = new JakartaServletFileUpload();
				try {
					FileItemInputIterator itemIterator = jakartaServletFileUpload.getItemIterator(httpServletRequest);
					MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
					if (itemIterator.hasNext()) {
						FileItemInput fileItemInput = itemIterator.next();
						FileItemHeaders fileItemInputHeaders = fileItemInput.getHeaders();

						System.out.println("Received file: Field: " + fileItemInput.getFieldName() + " Filename: " + fileItemInput.getName() + " content type: " + fileItemInput.getContentType());
						System.out.println("FileItemInputHeaders = " + fileItemInputHeaders);

						MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
						fileMap.add(HttpHeaders.CONTENT_DISPOSITION, fileItemInputHeaders.getHeader(HttpHeaders.CONTENT_DISPOSITION));
						fileMap.add(HttpHeaders.CONTENT_TYPE, fileItemInputHeaders.getHeader(HttpHeaders.CONTENT_TYPE));

						HttpEntity<InputStreamResource> entity = new HttpEntity<>(new InputStreamResource(fileItemInput.getInputStream()), fileMap);
						bodyMap.add(fileItemInput.getFieldName(), entity);
					}

					HttpHeaders httpHeaders = new HttpHeaders(requestHeaders);
					httpHeaders.remove("cookie");

					HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, httpHeaders);
					return restTemplate.postForEntity(URI.create("http://localhost:9000/receive"), requestEntity, String.class).getBody();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return "File uploaded";
	    }
	}
	

	public static void main(String[] args) {
		SpringApplication.run(Fileupload2Application.class, args);
	}

}
