package sandipchitale.fileupload2;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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
		}

	    @PostMapping("/upload")
	    public String upload(HttpServletRequest httpServletRequest) {
			boolean multipartContent = JakartaServletFileUpload.isMultipartContent(httpServletRequest);
			if (multipartContent) {
				JakartaServletFileUpload jakartaServletFileUpload = new JakartaServletFileUpload();
				try {
					FileItemInputIterator itemIterator = jakartaServletFileUpload.getItemIterator(httpServletRequest);
					if (itemIterator.hasNext()) {
						FileItemInput fileItemInput = itemIterator.next();
						MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
						bodyMap.add(fileItemInput.getName(), new InputStreamResource(fileItemInput.getInputStream()));

						RequestEntity<MultiValueMap<String, Object>> request =
								RequestEntity.post(URI.create("http://localhost:9000/receive"))
										.header(HttpHeaders.CONTENT_TYPE, fileItemInput.getContentType())
										.contentType(MediaType.MULTIPART_FORM_DATA)
										.body(bodyMap);
						return restTemplate.exchange(request, String.class).getBody();
					}
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
