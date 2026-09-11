package cl.duoc.barriodigital.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportApiSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void sin_token_401() throws Exception {
        mvc.perform(get("/report/kpis")).andExpect(status().isUnauthorized());
    }

    @Test
    void admin_puede_ver_kpis() throws Exception {
        mvc.perform(get("/report/kpis").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
           .andExpect(status().isOk());
    }

    @Test
    void funcionario_no_puede_ver_kpis_403() throws Exception {
        mvc.perform(get("/report/kpis").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Funcionario"))))
           .andExpect(status().isForbidden());
    }
}
