package com.vitu.order.service;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitu.order.dto.OrderEvent;
import com.vitu.order.entity.Order;
import com.vitu.order.repository.OrderRepo;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class ReverseOrder {

	@Autowired
	private OrderRepo repository;
	
	

	@KafkaListener(topics = "reversed-orders", groupId = "orders-group")
	public void reverseOrder(String event) {
		log.info("Inside reverse order for order {}",event);
		try {
			OrderEvent orderEvent = new ObjectMapper().readValue(event, OrderEvent.class);

		
			Optional<Order> order = repository.findById(orderEvent.getOrder().getOrderId());

			order.ifPresent(o -> {
				o.setStatus("FAILED");
				this.repository.save(o);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
