package com.treserve.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
        SELECT e FROM Event e
        WHERE e.status = 'ACTIVE'
          AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:category IS NULL OR e.category = :category)
        ORDER BY e.startTime ASC
    """)
    Page<Event> findActiveEvents(
        @Param("search") String search,
        @Param("category") String category,
        Pageable pageable
    );
}
