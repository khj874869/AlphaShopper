package com.webjpa.shopping;

import com.webjpa.shopping.search.ProductSearchRepository;
import com.webjpa.shopping.service.ProductSearchIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class ShoppingApplicationTests {

    @MockBean
    private ProductSearchIndexService productSearchIndexService;

    @MockBean
    private ProductSearchRepository productSearchRepository;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void contextLoads() {
    }
}
