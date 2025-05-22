package com.javation.coloringbook.Repository;

import com.javation.coloringbook.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByBookPayId(Long bookPayId);
}
