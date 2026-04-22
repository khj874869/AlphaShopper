package com.webjpa.shopping.repository;

import com.webjpa.shopping.domain.KafkaDltReplayAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaDltReplayAuditRepository extends JpaRepository<KafkaDltReplayAudit, Long> {

    Page<KafkaDltReplayAudit> findAllByOrderByRequestedAtDesc(Pageable pageable);
}
