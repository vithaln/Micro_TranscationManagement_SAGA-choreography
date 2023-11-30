package com.vitu.stock.entity.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitu.stock.dto.DeliveryEvent;
import com.vitu.stock.dto.PaymentEvent;
import com.vitu.stock.entity.WareHouse;
import com.vitu.stock.repository.StockRepository;

import lombok.extern.slf4j.Slf4j;


@RestController
@Slf4j
public class ReversesStock {

	@Autowired
	private StockRepository repository;

	@Autowired
	private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

	@KafkaListener(topics = "reversed-stock", groupId = "stock-group")
	public void reverseStock(String event) {
		log.info("Inside reverse stock for order "+event);
		
		try {
			DeliveryEvent deliveryEvent = new ObjectMapper().readValue(event, DeliveryEvent.class);

			Iterable<WareHouse> inv = this.repository.findByItem(deliveryEvent.getOrder().getItem());

			inv.forEach(i -> {
				i.setQuantity(i.getQuantity() + deliveryEvent.getOrder().getQuantity());
				repository.save(i);
			});

			PaymentEvent paymentEvent = new PaymentEvent();
			paymentEvent.setOrder(deliveryEvent.getOrder());
			paymentEvent.setType("PAYMENT_REVERSED");
			kafkaTemplate.send("reversed-payments", paymentEvent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
