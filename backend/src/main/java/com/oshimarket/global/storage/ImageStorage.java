package com.oshimarket.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 파일 저장소. 현재 구현은 Cloudinary(CloudinaryImageStorage) 하나지만,
 * 서비스 코드가 특정 업체 SDK에 직접 묶이지 않도록 인터페이스로 분리 (테스트에서도 mock으로 대체).
 */
public interface ImageStorage {

    StoredImage upload(MultipartFile file);

    void delete(String publicId);
}
