package com.arthium.finance.storage;

import com.arthium.finance.common.ApiException;
import com.arthium.finance.common.Json;
import com.arthium.finance.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;


@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final RestClient restClient;
    private final AppProperties properties;
    private final Json json;

    public CloudinaryService(RestClient outboundRestClient, AppProperties properties, Json json) {
        this.restClient = outboundRestClient;
        this.properties = properties;
        this.json = json;
    }

    public String uploadFile(byte[] fileBytes, String filename, String folder) {
        return uploadFile(fileBytes, filename, folder, "auto");
    }

    /** Uploads the file and returns its secure_url. */
    public String uploadFile(byte[] fileBytes, String filename, String folder, String resourceType) {
        AppProperties.Cloudinary config = properties.getCloudinary();

        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, String> signedParams = new TreeMap<>();
        signedParams.put("folder", folder);
        signedParams.put("timestamp", String.valueOf(timestamp));

        String signature = sign(signedParams, config.getApiSecret());

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new NamedByteArrayResource(fileBytes,
                filename != null && !filename.isBlank() ? filename : "upload"));
        form.add("api_key", config.getApiKey());
        form.add("timestamp", String.valueOf(timestamp));
        form.add("folder", folder);
        form.add("signature", signature);

        String url = "https://api.cloudinary.com/v1_1/" + config.getCloudName() + "/" + resourceType + "/upload";

        try {
            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> parsed = json.readMap(response);
            String secureUrl = Json.str(parsed, "secure_url");

            if (secureUrl == null || secureUrl.isBlank()) {
                throw ApiException.badRequest("Cloudinary upload failed");
            }
            return secureUrl;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw ApiException.badRequest("Failed to upload file");
        }
    }

    public void deleteFile(String publicId) {
        AppProperties.Cloudinary config = properties.getCloudinary();
        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, String> signedParams = new TreeMap<>();
        signedParams.put("public_id", publicId);
        signedParams.put("timestamp", String.valueOf(timestamp));

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("public_id", publicId);
        form.add("api_key", config.getApiKey());
        form.add("timestamp", String.valueOf(timestamp));
        form.add("signature", sign(signedParams, config.getApiSecret()));

        try {
            restClient.post()
                    .uri("https://api.cloudinary.com/v1_1/" + config.getCloudName() + "/image/destroy")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Cloudinary delete failed for {}", publicId, e);
        }
    }

    private static String sign(Map<String, String> params, String apiSecret) {
        StringBuilder toSign = new StringBuilder();
        params.forEach((key, value) -> {
            if (!toSign.isEmpty()) {
                toSign.append("&");
            }
            toSign.append(key).append("=").append(value);
        });
        toSign.append(apiSecret);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(toSign.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign Cloudinary request", e);
        }
    }

    /** Multipart parts need a filename, which a bare ByteArrayResource does not carry. */
    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
