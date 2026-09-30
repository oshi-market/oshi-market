package com.oshimarket.domain.item.controller;

import com.oshimarket.domain.item.dto.ItemImageResponse;
import com.oshimarket.domain.item.service.ItemImageService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 상품 사진 업로드/삭제. 상품 판매자 본인만 가능 (조회는 상품 조회 응답의 images에 포함). */
@RestController
@RequestMapping("/api/items/{itemId}/images")
public class ItemImageController {

    private final ItemImageService itemImageService;

    public ItemImageController(ItemImageService itemImageService) {
        this.itemImageService = itemImageService;
    }

    /** multipart/form-data, 파트 이름 files (여러 장 가능). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ItemImageResponse>> upload(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long itemId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemImageService.upload(itemId, memberId, files));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long itemId,
            @PathVariable Long imageId
    ) {
        itemImageService.delete(itemId, imageId, memberId);
        return ResponseEntity.noContent().build();
    }
}
