package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.entity.ProcessedParcelEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedParcelEventRepository extends JpaRepository<ProcessedParcelEvent, String> {
}
