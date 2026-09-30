package com.oshimarket.domain.curation.service;

import com.oshimarket.domain.curation.dto.CurationResponse;
import com.oshimarket.domain.curation.dto.CurationSearchCondition;
import com.oshimarket.domain.curation.entity.Curation;
import com.oshimarket.domain.curation.repository.CurationRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CurationService {

    private final CurationRepository curationRepository;

    public CurationService(CurationRepository curationRepository) {
        this.curationRepository = curationRepository;
    }

    public List<CurationResponse> search(CurationSearchCondition condition) {
        return curationRepository.search(condition.work(), condition.character()).stream()
                .map(CurationResponse::from)
                .toList();
    }

    public CurationResponse getCuration(Long curationId) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CURATION_NOT_FOUND));

        return CurationResponse.from(curation);
    }
}
