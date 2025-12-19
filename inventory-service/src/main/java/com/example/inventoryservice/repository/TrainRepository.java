package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
	Optional<Train> findByTrainNumber(String trainNumber);
}

