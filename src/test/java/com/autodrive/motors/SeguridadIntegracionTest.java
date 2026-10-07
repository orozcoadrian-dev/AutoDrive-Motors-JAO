package com.autodrive.motors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SeguridadIntegracionTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void redirigeALoginSiLaVistaNoTieneSesion() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void rechazaLaApiSinSesionDeAdministrador() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permiteConsultarLaApiTrasLoginDeAdministrador() throws Exception {
        MvcResult login = mockMvc.perform(formLogin()
                        .user("admin")
                        .password("admin-local-2026"))
                .andExpect(authenticated().withUsername("admin"))
                .andReturn();
        MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/clientes").session(sesion))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaCambiosSinTokenCsrfAunqueLaSesionSeaValida() throws Exception {
        MvcResult login = mockMvc.perform(formLogin()
                        .user("admin")
                        .password("admin-local-2026"))
                .andExpect(authenticated())
                .andReturn();
        MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/api/clientes")
                        .session(sesion)
                        .contentType("application/json")
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"documento\":\"1000000010\",\"email\":\"ana@example.test\",\"telefono\":\"3001234567\"}"))
                .andExpect(status().isForbidden());
    }
}
