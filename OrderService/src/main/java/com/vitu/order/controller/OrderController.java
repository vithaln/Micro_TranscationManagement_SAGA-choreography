package com.vitu.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vitu.order.dto.CustomerOrder;
import com.vitu.order.dto.OrderEvent;
import com.vitu.order.entity.Order;
import com.vitu.order.repository.OrderRepo;

@RestController
@RequestMapping("/order")
public class OrderController {
	
	@Autowired
	private OrderRepo repository;
	@Autowired
	private KafkaTemplate< String, OrderEvent> kafkaTemplate;

	@PostMapping("/orders")
	public void createOrder(@RequestBody CustomerOrder customerOrder) {
		Order order = new Order();

		try {
			//placing order into db
			
			order.setAmount(customerOrder.getAmount());
			order.setItem(customerOrder.getItem());
			order.setQuantity(customerOrder.getQuantity());
			order.setStatus("CREATED");
			order = repository.save(order);

			
			//send event to topic
			customerOrder.setOrderId(order.getId());
			OrderEvent event = new OrderEvent();
			event.setOrder(customerOrder);
			event.setType("ORDER_CREATED");
			kafkaTemplate.send("new-orders", event);
		} catch (Exception e) {
			order.setStatus("FAILED");
			repository.save(order);
		}
	}

}
