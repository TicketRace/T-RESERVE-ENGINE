package com.treserve.venue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.treserve.venue.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {
}
