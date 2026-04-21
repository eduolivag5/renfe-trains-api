package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.StopTime;
import com.renfetrains.renfetrains.entities.StopTimeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StopTimeRepository extends JpaRepository<StopTime, StopTimeId> {

    @Query("SELECT st FROM StopTime st JOIN FETCH st.stop WHERE st.trip.tripId = :tripId ORDER BY st.stopSequence ASC")
    List<StopTime> findItineraryWithStops(@Param("tripId") String tripId);
}