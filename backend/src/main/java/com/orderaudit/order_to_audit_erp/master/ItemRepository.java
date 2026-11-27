package com.orderaudit.order_to_audit_erp.master;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByActiveTrueOrderByIdAsc();

    List<Item> findByItemTypeAndActiveTrueOrderByIdAsc(ItemType itemType);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    // Row lock for the stock engine (Checkpoint 4): two dispatches cannot both pass the stock check
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> lockById(@Param("id") Long id);
}
