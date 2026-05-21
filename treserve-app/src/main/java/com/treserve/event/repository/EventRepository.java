package com.treserve.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.treserve.event.entity.Event;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query(value = """
        SELECT e FROM Event e
        JOIN FETCH e.venue
        WHERE e.status = 'ACTIVE'
          AND (:search = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:category = '' OR e.category = :category)
        ORDER BY e.startTime ASC
    """, countQuery = """
        SELECT COUNT(e) FROM Event e
        WHERE e.status = 'ACTIVE'
          AND (:search = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:category = '' OR e.category = :category)
    """)
    Page<Event> findActiveEvents(
        @Param("search") String search,
        @Param("category") String category,
        Pageable pageable
    );



    /** Мероприятие по ID с площадкой (без LazyInitializationException) */
    @Query("""
        SELECT e FROM Event e
        JOIN FETCH e.venue
        WHERE e.id = :id
    """)
    Optional<Event> findByIdWithVenue(@Param("id") Long id);
}