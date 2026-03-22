package com.example.coffeshopManagement.repository;

import com.example.coffeshopManagement.entity.TableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableEntityRepository extends JpaRepository<TableEntity, Integer> {
}
