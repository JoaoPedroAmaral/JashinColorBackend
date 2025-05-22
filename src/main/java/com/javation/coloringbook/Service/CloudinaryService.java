package com.javation.coloringbook.Service;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
    }

    public String uploadFile(byte[] bytes) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(bytes, ObjectUtils.emptyMap());
        return uploadResult.get("secure_url").toString();
    }

    public String uploadPdf(byte[] pdfBytes, String fileName) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(pdfBytes, ObjectUtils.asMap(
                "public_id", "books_pdf/" + fileName,
                "resource_type", "raw",
                "format", "pdf"
        ));
        return uploadResult.get("secure_url").toString();
    }

}
