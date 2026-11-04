package de.sgs.heirs;

import de.sgs.heirs.service.ServerMasterKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class HeirsApplicationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ServerMasterKeyService masterKeyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void testMasterPage() throws Exception {
        mockMvc.perform(get("/master"))
                .andExpect(status().isOk());
    }

    @Test
    void testFormLoginSuccess() throws Exception {
        String username = "admin";
        String password = "admin123";
        Path lockPath = Paths.get("wyarf.irs");
        if (Files.exists(lockPath)) {
            try (BufferedReader reader = Files.newBufferedReader(lockPath)) {
                String line = reader.readLine();
                if (line != null && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    username = masterKeyService.decryptWithMasterKey(parts[0].trim());
                    password = masterKeyService.decryptWithMasterKey(parts[1].trim());
                }
            }
        }

        mockMvc.perform(post("/master")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void testFormLoginFailure() throws Exception {
        mockMvc.perform(post("/master")
                        .param("username", "admin")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/logout?error=bad_credentials"));
    }

}
