package com.vitu.payment.repo;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vitu.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	public List<Payment> findByOrderId(long orderId);
}
