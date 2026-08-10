package com.smartstocks.product.service;

import org.springframework.web.multipart.MultipartFile;

public interface ITemplateImageService {

    /**
     * Uploads an image file to S3 storage under the "template-images/" folder
     * and returns its publicly accessible URL.
     *
     * @param file the image file to upload
     * @return the public URL of the uploaded image
     */
    String uploadImage(MultipartFile file);
}
