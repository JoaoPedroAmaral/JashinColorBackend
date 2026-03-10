package com.javation.coloringbook.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadPdf(byte[] pdfBytes, String fileName) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(pdfBytes,
                ObjectUtils.asMap("resource_type", "raw", "public_id", fileName));
        return (String) uploadResult.get("secure_url");
    }

    public String uploadPdf(java.io.File file, String fileName) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file,
                ObjectUtils.asMap("resource_type", "raw", "public_id", fileName));
        return (String) uploadResult.get("secure_url");
    }

    public String uploadImage(byte[] imageBytes) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(imageBytes,
                ObjectUtils.asMap("resource_type", "image"));
        return (String) uploadResult.get("secure_url");
    }
}
