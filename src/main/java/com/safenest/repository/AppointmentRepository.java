package com.safenest.repository;

import com.safenest.model.AppointmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<AppointmentRecord, Long> {

    List<AppointmentRecord> findByCounsellorIdOrderByAppointmentDateTimeDesc(Long counsellorId);

    List<AppointmentRecord> findByVictimIdOrderByAppointmentDateTimeDesc(Long victimId);
}
