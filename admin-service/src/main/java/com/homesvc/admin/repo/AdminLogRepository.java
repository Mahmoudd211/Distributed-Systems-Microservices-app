package com.homesvc.admin.repo;

import com.homesvc.admin.model.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    List<AdminLog> findAllByOrderByPerformedAtDesc();
}
