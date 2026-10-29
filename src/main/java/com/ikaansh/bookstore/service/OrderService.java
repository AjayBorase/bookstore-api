package com.ikaansh.bookstore.service;

import com.ikaansh.bookstore.dto.OrderItemRequest;
import com.ikaansh.bookstore.dto.OrderRequest;
import com.ikaansh.bookstore.dto.OrderResponse;
import com.ikaansh.bookstore.entity.*;
import com.ikaansh.bookstore.exception.ConflictException;
import com.ikaansh.bookstore.exception.ForbiddenException;
import com.ikaansh.bookstore.exception.InsufficientStockException;
import com.ikaansh.bookstore.exception.ResourceNotFoundException;
import com.ikaansh.bookstore.mapper.Mappers;
import com.ikaansh.bookstore.repository.BookRepository;
import com.ikaansh.bookstore.repository.CustomerRepository;
import com.ikaansh.bookstore.repository.OrderRepository;
import com.ikaansh.bookstore.repository.OrderSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Business rules for orders.
 *
 * requesterId: the id of the logged-in CUSTOMER, or null when the caller is an ADMIN.
 * A customer may only touch their own orders; an admin may touch all.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository,
                        BookRepository bookRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.bookRepository = bookRepository;
    }

    /**
     * Places an order atomically. Any exception (RuntimeException) rolls the whole transaction back,
     * so stock is never decremented for an order that failed half-way.
     */
    @Transactional
    public OrderResponse placeOrder(OrderRequest req, Long requesterId) {
        if (requesterId != null && !requesterId.equals(req.customerId())) {
            throw new ForbiddenException("You can only place orders for your own account");
        }
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", req.customerId()));

        // Merge duplicate lines (book 1 x2 + book 1 x3 = book 1 x5). TreeMap = ascending book id,
        // so every transaction locks rows in the same order -> no deadlocks between concurrent orders.
        Map<Long, Integer> wanted = new TreeMap<>();
        for (OrderItemRequest item : req.items()) {
            wanted.merge(item.bookId(), item.quantity(), Integer::sum);
        }

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PLACED);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Book book = bookRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Book", entry.getKey()));
            int quantity = entry.getValue();
            if (book.getStock() < quantity) {
                throw new InsufficientStockException(book.getTitle(), quantity, book.getStock());
            }
            book.setStock(book.getStock() - quantity); // managed entity -> UPDATE on commit
            order.addItem(new OrderItem(book, quantity, book.getPrice()));
            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotalAmount(total);

        return Mappers.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id, Long requesterId) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        assertOwner(order, requesterId);
        return Mappers.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(Long customerId, OrderStatus status, Long requesterId, Pageable pageable) {
        Long effectiveCustomerId = requesterId != null ? requesterId : customerId; // customers only see their own
        return orderRepository
                .findAll(OrderSpecifications.filter(effectiveCustomerId, status), pageable)
                .map(x -> Mappers.toResponse(x));
    }

    /** Cancels an order and puts the stock back. Cancelling twice is a 409. */
    @Transactional
    public OrderResponse cancelOrder(Long id, Long requesterId) {
        CustomerOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        assertOwner(order, requesterId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Order " + id + " is already cancelled");
        }

        List<OrderItem> items = new ArrayList<>(order.getItems());
        items.sort(Comparator.comparing((OrderItem i) -> i.getBook().getId()));
        for (OrderItem item : items) {
            Book book = bookRepository.findByIdForUpdate(item.getBook().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book", item.getBook().getId()));
            book.setStock(book.getStock() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return Mappers.toResponse(order);
    }

    private void assertOwner(CustomerOrder order, Long requesterId) {
        if (requesterId != null && !requesterId.equals(order.getCustomer().getId())) {
            throw new ForbiddenException("You do not have access to order " + order.getId());
        }
    }
}
