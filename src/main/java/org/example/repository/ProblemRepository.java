package org.example.repository;

import org.example.entity.Problem;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {
    List<Problem> findByLevel(Integer level);
    /**
     * 난이도 구간에서 랜덤 정렬(데이터베이스 random() 함수)로 1페이지만 가져온 뒤,
     * 서비스/디폴트 메서드에서 첫 건만 꺼내 씁니다.
     */
    @Query("""
        select p from Problem p
        where p.level between :min and :max
        order by function('random')
    """)
    List<Problem> pickRandomWithin(@Param("min") int min,
                                       @Param("max") int max,
                                       PageRequest pageable);

    /**
     * 단일 레벨에서 랜덤 1문제 반환(빈 리스트면 null)
     */
    default Problem pickRandomSingleLevel(int level) {
        var list = pickRandomWithin(level, level, PageRequest.of(0, 1));
        return list.isEmpty() ? null : list.get(0);
    }
}