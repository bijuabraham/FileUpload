import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.Headers;
import feign.RequestLine;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    private final ClamAVClient clamAVClient;

    @Value("${clamav.url}")
    private String clamAVUrl;

    public FileUploadController() {
        this.clamAVClient = Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .target(ClamAVClient.class, clamAVUrl); // Externalize ClamAV URL for better configuration
    }

    @PostMapping
    public ResponseEntity<String> uploadFiles(
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("phone") String phone,
            @RequestParam("email") String email,
            @RequestParam("files") List<MultipartFile> files) {
        
        if (files.size() > 5) {
            return ResponseEntity.badRequest().body("You can upload up to 5 files only.");
        }

        List<CompletableFuture<ResponseEntity<String>>> futures = files.stream().map(file ->
            CompletableFuture.supplyAsync(() -> {
                if (file.getSize() > 150 * 1024) {
                    return ResponseEntity.badRequest().body("Each file must be less than 150KB: " + file.getOriginalFilename());
                }
                try {
                    boolean hasVirus = performAntivirusCheck(file);
                    if (hasVirus) {
                        return ResponseEntity.badRequest().body("File contains a virus: " + file.getOriginalFilename());
                    }
                } catch (IOException e) {
                    return ResponseEntity.status(500).body("Error scanning file: " + file.getOriginalFilename());
                }
                return ResponseEntity.ok("File is clean: " + file.getOriginalFilename());
            })
        ).collect(Collectors.toList());

        List<ResponseEntity<String>> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        StringBuilder responseMessage = new StringBuilder();
        for (ResponseEntity<String> result : results) {
            responseMessage.append(result.getBody()).append("\n");
            if (!result.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(result.getStatusCode()).body(responseMessage.toString());
            }
        }

        // Process files or save them
        return ResponseEntity.ok(responseMessage.append("All files uploaded successfully").toString());
    }

    private boolean performAntivirusCheck(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        String response = clamAVClient.scan(fileBytes);
        return response.contains("FOUND");
    }

    interface ClamAVClient {
        @RequestLine("POST /")
        @Headers("Content-Type: application/octet-stream")
        String scan(byte[] fileBytes);
    }
}