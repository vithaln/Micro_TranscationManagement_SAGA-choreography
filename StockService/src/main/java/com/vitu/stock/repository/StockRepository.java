package com.vitu.stock.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.vitu.stock.entity.WareHouse;

public interface StockRepository extends JpaRepository<WareHouse, Long> {

	Iterable<WareHouse> findByItem(String item);
}
