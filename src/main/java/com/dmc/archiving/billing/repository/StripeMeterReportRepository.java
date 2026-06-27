package com.dmc.archiving.billing.repository;

import com.dmc.archiving.billing.model.MeterReportStatus;
import com.dmc.archiving.billing.model.StripeMeterReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StripeMeterReportRepository extends JpaRepository<StripeMeterReport, Long> {

    Optional<StripeMeterReport> findByIdempotencyKey(String idempotencyKey);

    /** The drain queue: outbox rows awaiting (or retrying) a push to Stripe, oldest first. */
    List<StripeMeterReport> findByStatusOrderByCreatedAtAsc(MeterReportStatus status);
}
