package com.example.Bookstore.service;

import com.example.Bookstore.entity.Cart;
import com.example.Bookstore.entity.Customer;

public interface PersistentCartService {
    Cart getOrCreate(Customer customer);
    Cart addItem(Customer customer, String bookId, int qty);
    Cart setQty(Customer customer, String cartItemId, int qty);
    void clear(Customer me); 
}

