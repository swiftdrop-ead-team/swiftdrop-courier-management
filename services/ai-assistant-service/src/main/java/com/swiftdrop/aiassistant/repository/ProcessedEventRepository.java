package com.swiftdrop.aiassistant.repository;

import com.swiftdrop.aiassistant.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
