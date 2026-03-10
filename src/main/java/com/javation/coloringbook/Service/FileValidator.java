package com.javation.coloringbook.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("File list cannot be empty");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("File size exceeds limit");
            }

            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                throw new IllegalArgumentException("Only JPEG and PNG files are allowed");
            }
        }
    }
}
