package com.treserve.venue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.treserve.venue.entity.Seat;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByVenueId(Long venueId);
}
