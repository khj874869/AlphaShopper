package com.webjpa.shopping.repository;

import com.webjpa.shopping.domain.AiInteractionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AiInteractionLogRepository extends JpaRepository<AiInteractionLog, Long>, JpaSpecificationExecutor<AiInteractionLog> {

    List<AiInteractionLog> findAllByOrderByRequestedAtDesc(Pageable pageable);
}
