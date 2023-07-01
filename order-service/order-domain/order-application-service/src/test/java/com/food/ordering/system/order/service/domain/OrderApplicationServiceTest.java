package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.ProductId;
import com.food.ordering.system.domain.valueobject.RestaurantId;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.dto.create.OrderAddress;
import com.food.ordering.system.order.service.domain.dto.create.OrderItem;
import com.food.ordering.system.order.service.domain.entity.Customer;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.ports.input.service.OrderApplicationService;
import com.food.ordering.system.order.service.domain.ports.output.repository.CustomerRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = OrderTestConfiguration.class)
public class OrderApplicationServiceTest {

  @Autowired
  private OrderApplicationService orderApplicationService;
  @Autowired
  private OrderDataMapper orderDataMapper;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private CustomerRepository customerRepository;
  @Autowired
  private RestaurantRepository restaurantRepository;

  private CreateOrderCommand createOrderCommand;
  private CreateOrderCommand createOrderCommandWrongPrice;
  private CreateOrderCommand createOrderCommandWrongProductPrice;
  private final UUID CUSTOMER_ID = UUID.fromString("7e459fca-3eb5-4ce0-91be-c730f6fffd04");
  private final UUID RESTAURANT_ID = UUID.fromString("dc074d23-ef23-44ed-9ccf-bf701220f302");
  private final UUID PRODUCT_ID = UUID.fromString("9bd1b243-b0e0-4f23-bb42-6616077132ee");
  private final UUID ORDER_ID = UUID.fromString("ec4d9f29-1996-4a96-b823-a54258e9a696");
  private final BigDecimal PRICE = new BigDecimal("200.00");

  @BeforeAll
  public void init() {
    createOrderCommand = CreateOrderCommand.builder().customerId(CUSTOMER_ID).restaurantId(RESTAURANT_ID).price(PRICE)
      .address(OrderAddress.builder().street("street_1").postalCode("1000AB").city("Paris").build()).items(List.of(
        OrderItem.builder().productId(PRODUCT_ID).quantity(1).price(new BigDecimal("50.00"))
          .subTotal(new BigDecimal("50.00")).build(),
        OrderItem.builder().productId(PRODUCT_ID).quantity(3).price(new BigDecimal("50.00"))
          .subTotal(new BigDecimal("150.00")).build())).build();

    createOrderCommandWrongPrice = CreateOrderCommand.builder().customerId(CUSTOMER_ID).restaurantId(RESTAURANT_ID)
      .price(new BigDecimal("250.00"))
      .address(OrderAddress.builder().street("street_1").postalCode("1000AB").city("Paris").build()).items(List.of(
        OrderItem.builder().productId(PRODUCT_ID).quantity(1).price(new BigDecimal("50.00"))
          .subTotal(new BigDecimal("50.00")).build(),
        OrderItem.builder().productId(PRODUCT_ID).quantity(3).price(new BigDecimal("50.00"))
          .subTotal(new BigDecimal("150.00")).build())).build();

    createOrderCommandWrongProductPrice = CreateOrderCommand.builder().customerId(CUSTOMER_ID)
      .restaurantId(RESTAURANT_ID).price(new BigDecimal("210.00"))
      .address(OrderAddress.builder().street("street_1").postalCode("1000AB").city("Paris").build()).items(List.of(
        OrderItem.builder().productId(PRODUCT_ID).quantity(1).price(new BigDecimal("60.00"))
          .subTotal(new BigDecimal("60.00")).build(),
        OrderItem.builder().productId(PRODUCT_ID).quantity(3).price(new BigDecimal("50.00"))
          .subTotal(new BigDecimal("150.00")).build())).build();

    Customer customer = new Customer();
    customer.setId(new CustomerId(CUSTOMER_ID));

    Restaurant restaurant = Restaurant.builder().restaurantId(new RestaurantId(RESTAURANT_ID)).products(
      List.of(new Product(new ProductId(PRODUCT_ID), "product-1", new Money(new BigDecimal("50.00"))),
        new Product(new ProductId(PRODUCT_ID), "product-2", new Money(new BigDecimal("50.00"))))).active(true).build();

    Order order = orderDataMapper.createOrderCommandToOrder(createOrderCommand);
    order.setId(new OrderId(ORDER_ID));

    when(customerRepository.findCustomer(CUSTOMER_ID)).thenReturn(Optional.of(customer));
    when(restaurantRepository.findRestaurantInformation(
      orderDataMapper.createOrderCommandToRestaurant(createOrderCommand))).thenReturn(Optional.of(restaurant));
    when(orderRepository.save(any(Order.class))).thenReturn(order);
  }

  @Test
  public void testCreateOrder() {
    CreateOrderResponse createOrderResponse = orderApplicationService.createOrder(createOrderCommand);
    assertEquals(OrderStatus.PENDING, createOrderResponse.getOrderStatus());
    assertEquals("Order created successfully.", createOrderResponse.getMessage());
    assertNotNull(createOrderResponse.getOrderTrackingId());
  }

  @Test
  public void testCreateOrderWithWrongTotalPrice() {
    OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
      () -> orderApplicationService.createOrder(createOrderCommandWrongPrice));
    assertEquals("Total price: 250.00 is not equal to Order items total: 200.00.", orderDomainException.getMessage());
  }

  @Test
  public void testCreateOrderWithWrongProductPrice() {
    OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
      () -> orderApplicationService.createOrder(createOrderCommandWrongProductPrice));
    assertEquals("Order item price: 60.00 is not valid for product " + PRODUCT_ID + ".",
      orderDomainException.getMessage());
  }

  @Test
  public void testCreateOrderWithPassiveRestaurant() {
    Restaurant restaurant = Restaurant.builder().restaurantId(new RestaurantId(RESTAURANT_ID)).products(
      List.of(new Product(new ProductId(PRODUCT_ID), "product-1", new Money(new BigDecimal("50.00"))),
        new Product(new ProductId(PRODUCT_ID), "product-2", new Money(new BigDecimal("50.00"))))).active(false).build();
    when(restaurantRepository.findRestaurantInformation(
      orderDataMapper.createOrderCommandToRestaurant(createOrderCommand))).thenReturn(Optional.of(restaurant));
    OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
      () -> orderApplicationService.createOrder(createOrderCommand));
    assertEquals("Restaurant with id " + RESTAURANT_ID + " is currently not active.",
      orderDomainException.getMessage());
  }
}
