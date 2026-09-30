package com.oshimarket.domain.work.service;

import com.oshimarket.domain.work.dto.WorkResponse;
import com.oshimarket.domain.work.entity.Work;
import com.oshimarket.domain.work.repository.WorkRepository;
import java.util.Comparator;
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

    /**
     * 드롭다운용 전체 목록. 수십~수백 건 수준이라 페이지네이션 없이 이름순으로 반환.
     * DB 정렬(ORDER BY name)은 컨테이너 collation에 따라 한글이 가나다순으로 안 나와서 Java에서 정렬
     * (한글 음절은 유니코드 순서가 곧 가나다순).
     */
    public List<WorkResponse> getWorks() {
        return workRepository.findAll().stream()
                .sorted(Comparator.comparing(Work::getName))
                .map(WorkResponse::from)
                .toList();
    }
}
