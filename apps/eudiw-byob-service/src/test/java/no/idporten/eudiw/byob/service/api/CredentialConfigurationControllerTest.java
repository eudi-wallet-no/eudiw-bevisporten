package no.idporten.eudiw.byob.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialConfigurations;
import no.idporten.eudiw.byob.service.model.CredentialMetadata;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;
import no.idporten.eudiw.byob.service.service.CredentialConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("When POST-ing and GETing data to the BYOB")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
class CredentialConfigurationControllerTest {


    private MockMvc mockMvc;

    @MockitoSpyBean
    private CredentialConfigurationService service;

    private CredentialConfigurationController controller;

    private Map<String, CredentialConfiguration> persistenceLayer;

    @Autowired
    public CredentialConfigurationControllerTest(MockMvc mockMvc, CredentialConfigurationService service) {
        this.mockMvc = mockMvc;
        this.service = service;
        this.controller = new CredentialConfigurationController(service);
    }

    @BeforeEach
    void setUp() {
        this.persistenceLayer = new HashMap<>();
    }

    @DisplayName("That the response is formatted correctly when POSTing correct data to the BYOB")
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

    private String getPostRequest(String vct) throws Exception {

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

    private String getPostResponse(String vctName) throws Exception {
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

    @DisplayName("that we get all the registered entries when calling GET to credential-configurations endpoint")
    @Test
    void getAllRequestTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CredentialConfiguration input1 = new CredentialConfiguration("example1_sd-jwt", "example1", "dc+sd-jwt", new ExampleCredentialData("bar"), new CredentialMetadata(new ArrayList<>(), new ArrayList<>()));
        CredentialConfiguration input2 = new CredentialConfiguration("example2_sd-jwt", "example2", "dc+sd-jwt", new ExampleCredentialData("bar"), new CredentialMetadata(new ArrayList<>(), new ArrayList<>()));
        CredentialConfiguration input3 = new CredentialConfiguration("example3_sd-jwt", "example3", "dc+sd-jwt", new ExampleCredentialData("bar"), new CredentialMetadata(new ArrayList<>(), new ArrayList<>()));
        persistenceLayer.put("example1", input1);
        persistenceLayer.put("example2", input2);
        persistenceLayer.put("example3", input3);
        Mockito.when(service.getAllEntries()).thenReturn(new CredentialConfigurations(persistenceLayer.values().stream().toList()));
        mockMvc.perform(get("/v1/credential-configurations"))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(new CredentialConfigurations(service.getAllEntries().credentialConfigurations()))));
    }

    @DisplayName("that it gives 404 when the id is not found")
    @Test
    void getByIdWhenIdFoesNotExistTest() throws Exception {
        mockMvc.perform(get("/v1/credential-configurations/{id}", "nonexistent"))
                .andExpect(status().isNotFound());
    }


    @DisplayName("that it gives 404 when the credentialConfigurationId is not found")
    @Test
    void searchByCredentialConfigurationIdWhenIdFoesNotExistTest() throws Exception {
        mockMvc.perform(get("/v1/credential-configurations/search").param("credentialConfigurationId", "nonexistent"))
                .andExpect(status().isNotFound());
    }
}
