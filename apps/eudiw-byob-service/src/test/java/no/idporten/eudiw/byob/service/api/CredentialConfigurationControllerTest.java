package no.idporten.eudiw.byob.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialConfigurations;
import no.idporten.eudiw.byob.service.model.CredentialMetadata;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;
import no.idporten.eudiw.byob.service.service.CredentialConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static no.idporten.eudiw.byob.service.service.CredentialConfigurationService.VCT_PREFIX;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Test BYOB CredentialConfigurationController")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
class CredentialConfigurationControllerTest {

    private final MockMvc mockMvc;

    @MockitoSpyBean
    private final CredentialConfigurationService service;

    private Map<String, CredentialConfiguration> persistenceLayer;

    @Autowired
    public CredentialConfigurationControllerTest(MockMvc mockMvc, CredentialConfigurationService service) {
        this.mockMvc = mockMvc;
        this.service = service;
    }

    @BeforeEach
    void setUp() {
        this.persistenceLayer = new HashMap<>();
    }

    @Nested
    @DisplayName("when calling POST to credential-configuration endpoint")
    class TestCreate {

        @DisplayName("and create credential-configuration with valid input then the response is 200 with created CredentialConfiguration as body")
        @Test
        void postRequestTest() throws Exception {

            ObjectMapper mapper = new ObjectMapper();
            CredentialConfiguration input = mapper.readValue(getPostRequest("bevisetmitt"), CredentialConfiguration.class);
            CredentialConfiguration output = mapper.readValue(getPostResponse("bevisetmitt"), CredentialConfiguration.class);
            mockMvc.perform(post("/v1/credential-configuration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(output)));
        }

        @DisplayName("and create credential-configuration with invalid input for display then the response is 400 with error message as body")
        @Test
        void postRequestTestWithInvalidDisplayData() throws Exception {

            ObjectMapper mapper = new ObjectMapper();
            CredentialConfiguration input = mapper.readValue(getPostRequest("bevis"), CredentialConfiguration.class);
            input.credentialMetadata().claims().getFirst().display().clear(); // remove all display entries to make it invalid
            mockMvc.perform(post("/v1/credential-configuration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("validation_error"));
        }

        private String getPostRequest(String vct) {

            return
                    """ 
                            {
                                "vct": "%s",
                                "format": "dc+sd-jwt",
                                %s,
                                %s
                            }
                            """.formatted(vct, getExampleCredentialMetadata(), getCredentialMetadata());
        }

        private String getPostResponse(String vctName) {
            String credentialConfigurationId = "net.eidas2sandkasse:" + vctName + "_sd_jwt_vc";
            String vct = "net.eidas2sandkasse:" + vctName;
            return
                    """ 
                            {
                                "credential_configuration_id": "%s",
                                "vct": "%s",
                                "format": "dc+sd-jwt",
                                %s,
                                %s
                            }
                            """.formatted(credentialConfigurationId, vct, getExampleCredentialMetadata(), getCredentialMetadata());
        }

        private String getExampleCredentialMetadata() {
            return """
                    "example_credential_data": {
                        "json": "todo: implement me"
                    }""";
        }

        private String getCredentialMetadata() {
            return """
                    "credential_metadata": {
                        "display": [
                            {
                                "name": "MinID PID",
                                "locale": "no",
                                "background_color": "#afcee9",
                                "text_color": "#002c54"
                            }
                        ],
                        "claims": [
                            {
                                "path": "foo",
                                "mandatory": true,
                                "display": [{
                                    "name": "MinID PID",
                                    "locale": "no",
                                    "background_color": "#afcee9",
                                    "text_color": "#002c54"
                                }]
                            }
                        ]
                    
                    }""";
        }

    }

    @Nested
    @DisplayName("when calling GET to credential-configurations endpoint")
    class TestGetAll {

        @DisplayName("to get all credential-configurations then return all registered entries with response 200")
        @Test
        void getAllRequestTest() throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            CredentialConfiguration input1 = createCredentialConfiguration("example1_sd-jwt", "example1");
            CredentialConfiguration input2 = createCredentialConfiguration("example2_sd-jwt", "example2");
            CredentialConfiguration input3 = createCredentialConfiguration("example3_sd-jwt", "example3");
            persistenceLayer.put("example1", input1);
            persistenceLayer.put("example2", input2);
            persistenceLayer.put("example3", input3);
            when(service.getAllEntries()).thenReturn(new CredentialConfigurations(persistenceLayer.values().stream().toList()));
            mockMvc.perform(get("/v1/credential-configurations"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(mapper.writeValueAsString(new CredentialConfigurations(service.getAllEntries().credentialConfigurations()))));
        }
    }

    @Nested
    @DisplayName("when calling GET to credential-configuration endpoint")
    class TestGetSingle {

        @DisplayName("with vct as path is should return response 200 with valid credentialConfiguration when the vct is found")
        @Test
        void getByIdWhenIdDoesExistTest() throws Exception {
            String vct = "example_sd-jwt";
            String credentialConfigurationId = VCT_PREFIX + vct;
            when(service.getCredentialConfiguration(eq(vct))).thenReturn(createCredentialConfiguration(credentialConfigurationId, vct));
            mockMvc.perform(get("/v1/credential-configuration/{id}", vct))
                    .andExpect(status().isOk())
            .andExpect(jsonPath("$.credential_configuration_id").value(credentialConfigurationId))
            .andExpect(jsonPath("$.vct").value(vct));
        }

        @DisplayName("with vct as path is should return response 404 when the id is not found")
        @Test
        void getByIdWhenIdDoesNotExistTest() throws Exception {
            mockMvc.perform(get("/v1/credential-configuration/{id}", "nonexistent"))
                    .andExpect(status().isNotFound());
        }

        @DisplayName("with search and credential_configuration_id as request parameter should return response 200 with valid credentialConfiguration when the credential_configuration_id is found")
        @Test
        void searchByCredentialConfigurationIdWhenIdDoesExistTest() throws Exception {
            String vct = "example_sd-jwt";
            String credentialConfigurationId = VCT_PREFIX + vct;
            when(service.searchCredentialConfiguration(eq(credentialConfigurationId))).thenReturn(createCredentialConfiguration(credentialConfigurationId, vct));
            mockMvc.perform(get("/v1/credential-configuration/search").param("credentialConfigurationId", credentialConfigurationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credential_configuration_id").value(credentialConfigurationId))
                    .andExpect(jsonPath("$.vct").value(vct));
        }

        @DisplayName("search with credential_configuration_id as request param should gives 404 when the credentialConfigurationId is not found")
        @Test
        void searchByCredentialConfigurationIdWhenIdDoesNotExistTest() throws Exception {
            mockMvc.perform(get("/v1/credential-configuration/search").param("credentialConfigurationId", "nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }

    private static CredentialConfiguration createCredentialConfiguration(String credentialConfigurationId, String vct) {
        return new CredentialConfiguration(credentialConfigurationId, vct, "dc+sd-jwt", new ExampleCredentialData("bar"), new CredentialMetadata(new ArrayList<>(), new ArrayList<>()));
    }

}
