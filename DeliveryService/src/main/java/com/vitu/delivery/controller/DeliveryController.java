package com.vitu.delivery.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitu.delivery.entity.Delivery;
import com.vitu.delivery.entity.dto.CustomerOrder;
import com.vitu.delivery.entity.dto.DeliveryEvent;
import com.vitu.delivery.repository.DeliveryRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class DeliveryController {

	@Autowired
	private DeliveryRepository repository;

	@Autowired
	private KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

	@KafkaListener(topics = "new-stock", groupId = "stock-group")
	public void deliverOrder(String event) throws JsonMappingException, JsonProcessingException {
		log.info("Inside ship order for order "+event);
		
		Delivery shipment = new Delivery();
		DeliveryEvent inventoryEvent = new ObjectMapper().readValue(event, DeliveryEvent.class);
		CustomerOrder order = inventoryEvent.getOrder();

		try {
			if (order.getAddress() == null) {
				throw new Exception("Address not present");
			}

			shipment.setAddress(order.getAddress());
			shipment.setOrderId(order.getOrderId());

			shipment.setStatus("success");

			repository.save(shipment);
		} catch (Exception e) {
			shipment.setOrderId(order.getOrderId());
			shipment.setStatus("failed");
			repository.save(shipment);

			System.out.println(order);

			DeliveryEvent reverseEvent = new DeliveryEvent();
			reverseEvent.setType("STOCK_REVERSED");
			reverseEvent.setOrder(order);
			kafkaTemplate.send("reversed-stock", reverseEvent);
		}
	}
}
