package com.oshimarket.domain.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.curation.dto.CurationResponse;
import com.oshimarket.domain.curation.dto.CurationSearchCondition;
import com.oshimarket.domain.curation.entity.Curation;
import com.oshimarket.domain.curation.repository.CurationRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CurationServiceTest {

    private CurationRepository curationRepository;
    private CurationService curationService;

    @BeforeEach
    void setUp() {
        curationRepository = mock(CurationRepository.class);
        curationService = new CurationService(curationRepository);
    }

    @Test
    void search_필터에_맞는_목록을_반환한다() {
        Curation curation = withId(sampleCuration(), 1L);
        when(curationRepository.search("나루토", null)).thenReturn(List.of(curation));

        List<CurationResponse> responses = curationService.search(new CurationSearchCondition("나루토", null));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).storeName()).isEqualTo(curation.getStoreName());
    }

    @Test
    void search_조건이_없으면_전체_목록을_반환한다() {
        when(curationRepository.search(null, null)).thenReturn(List.of(withId(sampleCuration(), 1L)));

        List<CurationResponse> responses = curationService.search(new CurationSearchCondition(null, null));

        assertThat(responses).hasSize(1);
    }

    @Test
    void getCuration_존재하면_상세정보를_반환한다() {
        Curation curation = withId(sampleCuration(), 1L);
        when(curationRepository.findById(1L)).thenReturn(Optional.of(curation));

        CurationResponse response = curationService.getCuration(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.workTag()).isEqualTo("나루토");
    }

    @Test
    void getCuration_존재하지_않으면_예외가_발생한다() {
        when(curationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> curationService.getCuration(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CURATION_NOT_FOUND);
    }

    private static Curation sampleCuration() {
        try {
            var constructor = Curation.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** BIGSERIAL id, work_tag 등은 리플렉션으로 채워서 저장 이후 상태를 흉내낸다 (DB 없이 단위 테스트). */
    private static Curation withId(Curation curation, Long id) {
        try {
            setField(curation, "id", id);
            setField(curation, "workTag", "나루토");
            setField(curation, "storeName", "애니메이트 공식몰");
            setField(curation, "url", "https://example.com/naruto-goods");
            return curation;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = Curation.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
