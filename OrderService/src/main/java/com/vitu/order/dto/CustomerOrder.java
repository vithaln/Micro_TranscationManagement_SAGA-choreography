package com.vitu.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerOrder {

	private String item;

	private int quantity;

	private double amount;

	private String paymentMode;

	private long orderId;

	private String address;

}
