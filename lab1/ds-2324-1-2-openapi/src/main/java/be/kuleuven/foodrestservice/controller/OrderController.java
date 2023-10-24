package be.kuleuven.foodrestservice.controller;

import be.kuleuven.foodrestservice.api.OrdersApi;
import be.kuleuven.foodrestservice.domain.MealsRepository;
import be.kuleuven.foodrestservice.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
public class OrderController implements OrdersApi {

    private static final MealsRepository mealsRepository = new MealsRepository();

    @Override
    public ResponseEntity<Object> addOrder(OrderRequest orderRequest){
        Order order = mealsRepository.addOrder(orderRequest);

        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path(order.getId().toString()).build().toUri()).body(order);
    }

    @Override
    public ResponseEntity<List<Order>> getOrders(){
        return ResponseEntity.ok(mealsRepository.getAllOrders());
    }

}
