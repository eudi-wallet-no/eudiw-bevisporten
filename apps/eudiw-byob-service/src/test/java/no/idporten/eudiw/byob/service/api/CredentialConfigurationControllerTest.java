package no.idporten.eudiw.byob.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.byob.service.model.*;
import no.idporten.eudiw.byob.service.serviceClasses.CredentialConfigurationService;
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
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When POST-ing and GETing data to the BYOB")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
class CredentialConfigurationControllerTest {

    private final String example = """
            {
            "vct": "foomittbevisheisenleisensen",
            "format": "dc+sd-jwt",
            "example_credential_metadata": {
        "foo": "bar"
    },
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
                "display": {
            "name": "MinID PID",
                "locale": "no",
                "background_color": "#afcee9",
                "text_color": "#002c54"
        }
        }
    ]
    }
}""";

    private MockMvc mockMvc;

    @MockitoSpyBean
    private CredentialConfigurationService service;

    private CredentialConfigurationController controller;

    private Map<String, ByobInput> persistenceLayer;

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
        ByobInput input = mapper.readValue(example, ByobInput.class);
        Map<String, ByobInput> testMap = new HashMap<>();
        List<String> vcts = new ArrayList<>();
        vcts.add("mockvctthisisempty");
        String id = "net.eidas2sandkasse:" + input.vct() + 0 + "_sd_jwt_vc";
        testMap.put(id, input);
        mockMvc.perform(post("/v1/credential-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(testMap)));
    }

    @DisplayName("that we get all the registered entries when calling GET to credential-configurations endpoint")
    @Test
    void getAllRequestTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ByobInput input1 = new ByobInput("example1", "dc+sd-jwt", new ExampleCredentialMetadata("bar"), new CredentialMetadata(new ArrayList<Display>(), new ArrayList<Claims>()));
        ByobInput input2 = new ByobInput("example2", "dc+sd-jwt", new ExampleCredentialMetadata("bar"), new CredentialMetadata(new ArrayList<Display>(), new ArrayList<Claims>()));
        ByobInput input3 = new ByobInput("example3", "dc+sd-jwt", new ExampleCredentialMetadata("bar"), new CredentialMetadata(new ArrayList<Display>(), new ArrayList<Claims>()));
        persistenceLayer.put("example1", input1);
        persistenceLayer.put("example2", input2);
        persistenceLayer.put("example3", input3);
        Mockito.when(service.getAllEntries()).thenReturn(new ResponseTopObject(persistenceLayer.values().stream().toList()));
        mockMvc.perform(get("/v1/credential-configurations"))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(new ResponseTopObject(service.getAllEntries().byobs()))));
    }

    @DisplayName("that it gives 404 when the id is not found")
    @Test
    void getByIdWhenIdFoesNotExistTest() throws Exception {
        mockMvc.perform(get("/v1/credential-configurations/{id}", "nonexistent"))
                .andExpect(status().isNotFound());
    }
}
