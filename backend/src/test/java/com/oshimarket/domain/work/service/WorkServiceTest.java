package com.oshimarket.domain.work.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.work.dto.WorkResponse;
import com.oshimarket.domain.work.entity.Work;
import com.oshimarket.domain.work.repository.WorkRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkServiceTest {

    private WorkRepository workRepository;
    private WorkService workService;

    @BeforeEach
    void setUp() {
        workRepository = mock(WorkRepository.class);
        workService = new WorkService(workRepository);
    }

    @Test
    void getWorks_가나다순_목록을_반환한다() {
        when(workRepository.findAll()).thenReturn(List.of(Work.of("원신"), Work.of("나루토"), Work.of("귀멸의 칼날")));

        List<WorkResponse> works = workService.getWorks();

        assertThat(works).extracting(WorkResponse::name).containsExactly("귀멸의 칼날", "나루토", "원신");
    }
}
