package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.dtos.TrainSearchResultDTO;
import com.renfetrains.renfetrains.entities.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, String> {

    @Query(value = """
    SELECT new com.renfetrains.renfetrains.dtos.TrainSearchResultDTO(
        t.tripId, 
        st1.departureTime, 
        st2.arrivalTime, 
        r.tipoTren, 
        r.longName
    )
    FROM Trip t
    JOIN t.route r
    JOIN StopTime st1 ON t.tripId = st1.trip.tripId
    JOIN StopTime st2 ON t.tripId = st2.trip.tripId
    JOIN Calendar c ON t.serviceId = c.serviceId
    WHERE st1.stop.stopId = :originId
      AND st2.stop.stopId = :destinationId
      AND st1.stopSequence < st2.stopSequence
      AND :date BETWEEN c.startDate AND c.endDate
      AND (
        (:dayOfWeek = 1 AND c.monday = 1) OR
        (:dayOfWeek = 2 AND c.tuesday = 1) OR
        (:dayOfWeek = 3 AND c.wednesday = 1) OR
        (:dayOfWeek = 4 AND c.thursday = 1) OR
        (:dayOfWeek = 5 AND c.friday = 1) OR
        (:dayOfWeek = 6 AND c.saturday = 1) OR
        (:dayOfWeek = 7 AND c.sunday = 1)
      )
      AND (:startTime IS NULL OR st1.departureTime >= :startTime)
      AND (:tipoTren IS NULL OR r.tipoTren = :tipoTren)
    ORDER BY st1.departureTime ASC
    """)
    List<TrainSearchResultDTO> findTrainsWithFilters(
            @Param("originId") String originId,
            @Param("destinationId") String destinationId,
            @Param("date") String date,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("startTime") String startTime,
            @Param("tipoTren") String tipoTren
    );

}