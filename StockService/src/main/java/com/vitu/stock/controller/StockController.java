package com.vitu.stock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitu.stock.dto.CustomerOrder;
import com.vitu.stock.dto.DeliveryEvent;
import com.vitu.stock.dto.PaymentEvent;
import com.vitu.stock.dto.Stock;
import com.vitu.stock.entity.WareHouse;
import com.vitu.stock.repository.StockRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class StockController {

	@Autowired
	private StockRepository repository;

	@Autowired
	private KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

	@Autowired
	private KafkaTemplate<String, PaymentEvent> kafkaPaymentTemplate;

	@KafkaListener(topics = "new-payments", groupId = "payments-group")
	public void updateStock(String paymentEvent) throws JsonMappingException, JsonProcessingException {
		
		log.info("Payement event recieved {}",paymentEvent);
		
		DeliveryEvent event = new DeliveryEvent();

		PaymentEvent p = new ObjectMapper().readValue(paymentEvent, PaymentEvent.class);
		CustomerOrder order = p.getOrder();

		try {
			Iterable<WareHouse> inventories = repository.findByItem(order.getItem());

			boolean exists = inventories.iterator().hasNext();

			if (!exists) {
				System.out.println("Stock not exist so reverting the order");
				throw new Exception("Stock not available");
			}

			inventories.forEach(i -> {
				//present quantity-purchased quantity
				i.setQuantity(i.getQuantity() - order.getQuantity());

				repository.save(i);
			});

			event.setType("STOCK_UPDATED");
			event.setOrder(p.getOrder());
			kafkaTemplate.send("new-stock", event);
		} catch (Exception e) {
			PaymentEvent pe = new PaymentEvent();
			pe.setOrder(order);
			pe.setType("PAYMENT_REVERSED");
			kafkaPaymentTemplate.send("reversed-payments", pe);
		}
	}

	@PostMapping("/addItems")
	public void addItems(@RequestBody Stock stock) {
		
		//check if items are present increase it by how many we want to add extra.
		Iterable<WareHouse> items = repository.findByItem(stock.getItem());

		if (items.iterator().hasNext()) {
			items.forEach(i -> {
				i.setQuantity(stock.getQuantity() + i.getQuantity());
				repository.save(i);
			});
		} else {
			//if items are not present just add it in db.
			WareHouse i = new WareHouse();
			i.setItem(stock.getItem());
			i.setQuantity(stock.getQuantity());
			repository.save(i);
		}
	}
}
