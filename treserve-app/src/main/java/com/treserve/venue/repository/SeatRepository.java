package com.treserve.venue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.treserve.venue.entity.Seat;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

        /**
     * Получить общее количество мест в зале
     */
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.venue.id = :venueId")
    long countByVenueId(@Param("venueId") Long venueId);

    List<Seat> findByVenueId(Long venueId);
}
