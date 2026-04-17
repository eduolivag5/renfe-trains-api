package com.renfetrains.renfetrains.repositories;

import com.renfetrains.renfetrains.entities.TrenEnVivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrenRepository extends JpaRepository<TrenEnVivo, String> {
}