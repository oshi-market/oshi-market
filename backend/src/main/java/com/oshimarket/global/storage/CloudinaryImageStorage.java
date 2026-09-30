package com.oshimarket.global.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cloudinary 업로드/삭제. API Secret은 서버 환경변수(CLOUDINARY_URL)로만 받고 프론트에는 절대 노출하지 않는다
 * (브라우저 직접 업로드용 unsigned preset도 아무나 업로드할 수 있게 돼서 쓰지 않음).
 *
 * CLOUDINARY_URL이 없어도 서버는 뜨게 두고(사진 기능을 안 쓰는 팀원 로컬 환경), 실제 업로드/삭제 시점에 에러를 낸다.
 */
@Component
public class CloudinaryImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryImageStorage.class);

    private final Cloudinary cloudinary;
    private final String folder;

    public CloudinaryImageStorage(
            @Value("${cloudinary.url:}") String cloudinaryUrl,
            @Value("${cloudinary.folder}") String folder
    ) {
        this.cloudinary = StringUtils.hasText(cloudinaryUrl) ? new Cloudinary(cloudinaryUrl) : null;
        this.folder = folder;
        if (cloudinary == null) {
            log.warn("CLOUDINARY_URL이 설정되지 않아 사진 업로드를 사용할 수 없습니다. backend/.env.example 참고");
        }
    }

    @Override
    public StoredImage upload(MultipartFile file) {
        try {
            Map<?, ?> result = requireCloudinary().uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"
            ));
            return new StoredImage((String) result.get("secure_url"), (String) result.get("public_id"));
        } catch (IOException | RuntimeException e) {
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            log.error("Cloudinary 업로드 실패: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            requireCloudinary().uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException | RuntimeException e) {
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            log.error("Cloudinary 삭제 실패: {}", publicId, e);
            throw new BusinessException(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    private Cloudinary requireCloudinary() {
        if (cloudinary == null) {
            throw new BusinessException(ErrorCode.IMAGE_STORAGE_NOT_CONFIGURED);
        }
        return cloudinary;
    }
}
