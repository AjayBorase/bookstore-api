package com.ikaansh.bookstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikaansh.bookstore.entity.Book;
import com.ikaansh.bookstore.entity.Customer;
import com.ikaansh.bookstore.entity.Role;
import com.ikaansh.bookstore.repository.BookRepository;
import com.ikaansh.bookstore.repository.CustomerRepository;
import com.ikaansh.bookstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the order flow: HTTP -> controller -> service -> JPA -> H2.
 * Not @Transactional on purpose: each request commits/rolls back like in production,
 * which is what lets us prove that a failed order really leaves the stock untouched.
 */
@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class OrderPlacementIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BookRepository books;
    @Autowired CustomerRepository customers;
    @Autowired OrderRepository orders;

    Customer alice;
    Book effectiveJava;   // price 45.00, stock 10
    Book springBook;      // price 40.00, stock 5

    @BeforeEach
    void setUp() {
        orders.deleteAll();
        books.deleteAll();
        customers.deleteAll();
        alice = customers.save(newCustomer("Alice", "alice@example.com"));
        effectiveJava = books.save(newBook("Effective Java", "9780134685991", "45.00", 10));
        springBook = books.save(newBook("Spring in Action", "9781617297571", "40.00", 5));
    }

    // ---------- core order placement scenarios required by the assignment ----------

    @Test
    void placeOrder_success_savesOrderAndDecrementsStock() throws Exception {
        MvcResult result = placeOrder(admin(), alice.getId(), effectiveJava.getId(), 3, springBook.getId(), 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.customerId").value(alice.getId()))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        // 3 x 45.00 + 1 x 40.00 = 175.00
        assertThat(body.get("totalAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("175.00"));
        assertThat(stockOf(effectiveJava)).isEqualTo(7);
        assertThat(stockOf(springBook)).isEqualTo(4);
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void placeOrder_insufficientStock_returns409AndChangesNothing() throws Exception {
        placeOrder(admin(), alice.getId(), effectiveJava.getId(), 11)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.path").value("/api/orders"));

        assertThat(stockOf(effectiveJava)).isEqualTo(10);
        assertThat(orders.count()).isZero();
    }

    @Test
    void placeOrder_invalidCustomer_returns404() throws Exception {
        placeOrder(admin(), 999_999L, effectiveJava.getId(), 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        assertThat(stockOf(effectiveJava)).isEqualTo(10);
        assertThat(orders.count()).isZero();
    }

    // ---------- extra edge cases ----------

    @Test
    void placeOrder_unknownBook_returns404() throws Exception {
        placeOrder(admin(), alice.getId(), 999_999L, 1)
                .andExpect(status().isNotFound());
        assertThat(orders.count()).isZero();
    }

    @Test
    void placeOrder_zeroQuantity_returns400WithFieldDetails() throws Exception {
        placeOrder(admin(), alice.getId(), effectiveJava.getId(), 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void placeOrder_emptyItems_returns400() throws Exception {
        mvc.perform(post("/api/orders").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + alice.getId() + ",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_secondItemOutOfStock_rollsBackFirstItem() throws Exception {
        // 2 x Effective Java is fine, 6 x Spring (stock 5) fails -> the whole order must roll back
        placeOrder(admin(), alice.getId(), effectiveJava.getId(), 2, springBook.getId(), 6)
                .andExpect(status().isConflict());

        assertThat(stockOf(effectiveJava)).isEqualTo(10);
        assertThat(stockOf(springBook)).isEqualTo(5);
        assertThat(orders.count()).isZero();
    }

    @Test
    void placeOrder_duplicateLinesAreMergedBeforeStockCheck() throws Exception {
        // 6 + 5 = 11 of the same book, only 10 in stock
        placeOrder(admin(), alice.getId(), effectiveJava.getId(), 6, effectiveJava.getId(), 5)
                .andExpect(status().isConflict());
        assertThat(stockOf(effectiveJava)).isEqualTo(10);
    }

    @Test
    void cancelOrder_restoresStock_andSecondCancelIs409() throws Exception {
        MvcResult placed = placeOrder(admin(), alice.getId(), effectiveJava.getId(), 4)
                .andExpect(status().isCreated()).andReturn();
        long orderId = objectMapper.readTree(placed.getResponse().getContentAsString()).get("id").asLong();
        assertThat(stockOf(effectiveJava)).isEqualTo(6);

        mvc.perform(post("/api/orders/" + orderId + "/cancel").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stockOf(effectiveJava)).isEqualTo(10);

        mvc.perform(post("/api/orders/" + orderId + "/cancel").with(admin()))
                .andExpect(status().isConflict());
        assertThat(stockOf(effectiveJava)).isEqualTo(10); // not restored twice
    }

    @Test
    void getOrder_unknownId_returns404() throws Exception {
        mvc.perform(get("/api/orders/999999").with(admin()))
                .andExpect(status().isNotFound());
    }

    // ---------- security (bonus) ----------

    @Test
    void placeOrder_withoutToken_returns401() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"items\":[{\"bookId\":1,\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void customerRole_canOnlyActOnOwnOrders_andCannotManageBooks() throws Exception {
        JsonNode bob = register("Bob", "bob@example.com");
        JsonNode carol = register("Carol", "carol@example.com");
        RequestPostProcessor asBob = bearer(bob.get("token").asText());
        long bobId = bob.get("customerId").asLong();
        long carolId = carol.get("customerId").asLong();

        // Bob can order for himself ...
        placeOrder(asBob, bobId, effectiveJava.getId(), 1).andExpect(status().isCreated());

        // ... but not for Carol
        placeOrder(asBob, carolId, effectiveJava.getId(), 1).andExpect(status().isForbidden());

        // Carol's order (made by an admin) is invisible to Bob
        MvcResult carolOrder = placeOrder(admin(), carolId, springBook.getId(), 1)
                .andExpect(status().isCreated()).andReturn();
        long carolOrderId = objectMapper.readTree(carolOrder.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/orders/" + carolOrderId).with(asBob)).andExpect(status().isForbidden());

        // Bob's order list only contains his own order
        mvc.perform(get("/api/orders").with(asBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // A CUSTOMER cannot create books, an unauthenticated visitor can still browse them
        mvc.perform(post("/api/books").with(asBob).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"author\":\"Y\",\"isbn\":\"1\",\"price\":1.00,\"stock\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/books")).andExpect(status().isOk());
    }

    @Test
    void login_withWrongPassword_returns401_andWithRightPasswordReturnsToken() throws Exception {
        register("Dave", "dave@example.com");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dave@example.com\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dave@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // ---------- helpers ----------

    private ResultActions placeOrder(RequestPostProcessor auth, long customerId, long... bookIdQuantityPairs)
            throws Exception {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < bookIdQuantityPairs.length; i += 2) {
            if (i > 0) items.append(",");
            items.append("{\"bookId\":%d,\"quantity\":%d}".formatted(bookIdQuantityPairs[i], bookIdQuantityPairs[i + 1]));
        }
        String body = "{\"customerId\":%d,\"items\":[%s]}".formatted(customerId, items);
        return mvc.perform(post("/api/orders").with(auth).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode register(String name, String email) throws Exception {
        String body = "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"secret123\"}".formatted(name, email);
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private static RequestPostProcessor admin() {
        return user("admin@bookstore.com").roles("ADMIN");
    }

    private static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private int stockOf(Book book) {
        return books.findById(book.getId()).orElseThrow().getStock();
    }

    private static Customer newCustomer(String name, String email) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        c.setPassword("not-used-in-these-tests");
        c.setRole(Role.CUSTOMER);
        return c;
    }

    private static Book newBook(String title, String isbn, String price, int stock) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor("Some Author");
        b.setIsbn(isbn);
        b.setPrice(new BigDecimal(price));
        b.setStock(stock);
        b.setCategory("Programming");
        return b;
    }
}
