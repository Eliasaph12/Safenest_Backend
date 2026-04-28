package com.safenest.repository;

import com.safenest.model.OtpVerificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerificationRecord, Long> {

    Optional<OtpVerificationRecord> findByIdAndConsumedFalse(Long id);

    List<OtpVerificationRecord> findByEmailAddressAndPurposeAndConsumedFalse(String emailAddress, String purpose);

    List<OtpVerificationRecord> findByPhoneNumberAndPurposeAndConsumedFalse(String phoneNumber, String purpose);
}
