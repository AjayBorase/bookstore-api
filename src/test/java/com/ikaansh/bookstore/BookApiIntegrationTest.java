package com.ikaansh.bookstore;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CRUD, validation, duplicate handling, pagination and filtering for /api/books. */
@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class BookApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired BookRepository books;
    @Autowired CustomerRepository customers;
    @Autowired OrderRepository orders;

    private static final RequestPostProcessor ADMIN = user("admin@bookstore.com").roles("ADMIN");

    @BeforeEach
    void clean() {
        orders.deleteAll();
        books.deleteAll();
        customers.deleteAll();
    }

    private static String bookJson(String title, String isbn, String price, int stock, String category) {
        return "{\"title\":\"%s\",\"author\":\"Some Author\",\"isbn\":\"%s\",\"price\":%s,\"stock\":%d,\"category\":\"%s\"}"
                .formatted(title, isbn, price, stock, category);
    }

    private void create(String title, String isbn, String price, int stock, String category) throws Exception {
        mvc.perform(post("/api/books").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(title, isbn, price, stock, category)))
                .andExpect(status().isCreated());
    }

    @Test
    void crudLifecycle() throws Exception {
        String response = mvc.perform(post("/api/books").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("Clean Code", "111", "40.00", 5, "Programming")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();

        mvc.perform(get("/api/books/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("111"));

        mvc.perform(put("/api/books/" + id).with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("Clean Code 2nd", "111", "42.00", 9, "Programming")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code 2nd"))
                .andExpect(jsonPath("$.stock").value(9));

        mvc.perform(delete("/api/books/" + id).with(ADMIN)).andExpect(status().isNoContent());
        mvc.perform(get("/api/books/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateIsbn_returns409() throws Exception {
        create("A", "222", "10.00", 1, "X");
        mvc.perform(post("/api/books").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("B", "222", "10.00", 1, "X")))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidBook_returns400() throws Exception {
        mvc.perform(post("/api/books").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("", "333", "-5", -1, "X")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void malformedJson_returns400InStandardFormat() throws Exception {
        mvc.perform(post("/api/books").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listSupportsPaginationAndFilters() throws Exception {
        create("Java Basics", "1", "20.00", 5, "Programming");
        create("Java Advanced", "2", "50.00", 0, "Programming");
        create("World History", "3", "30.00", 7, "History");

        // pagination
        mvc.perform(get("/api/books?page=0&size=2&sort=id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        // filters
        mvc.perform(get("/api/books?title=java")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/books?category=history")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/books?inStock=true")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/books?minPrice=25&maxPrice=60")).andExpect(jsonPath("$.totalElements").value(2));

        // invalid sort field -> 400, not 500
        mvc.perform(get("/api/books?sort=doesNotExist,asc")).andExpect(status().isBadRequest());
    }
}
