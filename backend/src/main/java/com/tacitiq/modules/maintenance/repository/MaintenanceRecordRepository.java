package com.tacitiq.modules.maintenance.repository;

import com.tacitiq.modules.maintenance.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {
    List<MaintenanceRecord> findByAssetId(UUID assetId);
    Optional<MaintenanceRecord> findByWorkOrderNo(String workOrderNo);
}
