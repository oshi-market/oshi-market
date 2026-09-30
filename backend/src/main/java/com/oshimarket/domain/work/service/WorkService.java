package com.oshimarket.domain.work.service;

import com.oshimarket.domain.work.dto.WorkResponse;
import com.oshimarket.domain.work.repository.WorkRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkService {

    private final WorkRepository workRepository;

    public WorkService(WorkRepository workRepository) {
        this.workRepository = workRepository;
    }

    /** 드롭다운용 전체 목록. 수십~수백 건 수준이라 페이지네이션 없이 가나다순으로 반환. */
    public List<WorkResponse> getWorks() {
        return workRepository.findAllByOrderByNameAsc().stream()
                .map(WorkResponse::from)
                .toList();
    }
}
