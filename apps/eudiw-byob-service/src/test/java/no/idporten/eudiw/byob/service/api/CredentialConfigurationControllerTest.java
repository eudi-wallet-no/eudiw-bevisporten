package no.idporten.eudiw.byob.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.byob.service.data.RedisService;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialConfigurations;
import no.idporten.eudiw.byob.service.model.CredentialMetadata;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;
import no.idporten.eudiw.byob.service.model.data.CredentialConfigurationData;
import no.idporten.eudiw.byob.service.model.data.CredentialMetadataData;
import no.idporten.eudiw.byob.service.model.data.ExampleCredentialDataData;
import no.idporten.eudiw.byob.service.model.web.CredentialConfigurationRequestResource;
import no.idporten.eudiw.byob.service.service.CredentialConfigurationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static no.idporten.eudiw.byob.service.service.CredentialConfigurationService.VCT_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Test BYOB CredentialConfigurationController")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
class CredentialConfigurationControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private final RedisService redisService;

    @MockitoSpyBean
    private final CredentialConfigurationService service;

    @Autowired
    public CredentialConfigurationControllerTest(MockMvc mockMvc, CredentialConfigurationService service, RedisService redisService) {
        this.mockMvc = mockMvc;
        this.service = service;
        this.redisService = redisService;
    }

    @Nested
    @DisplayName("when calling POST to credential-configuration endpoint")
    class TestCreate {

        @DisplayName("and create credential-configuration with valid input then the response is 201 with created CredentialConfiguration as body")
        @Test
        void postRequestTest() throws Exception {

            ObjectMapper mapper = new ObjectMapper();
            CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest("bevisetmitt"), CredentialConfigurationRequestResource.class);
            CredentialConfiguration output = mapper.readValue(getPostResponse("bevisetmitt"), CredentialConfiguration.class);
            mockMvc.perform(post("/v1/credential-configuration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isCreated())
                    .andExpect(content().json(mapper.writeValueAsString(output)));

            verify(redisService).addBevisType(any(CredentialConfigurationData.class));
        }

        @DisplayName("and create credential-configuration with invalid input for display then the response is 400 with error message as body")
        @Test
        void postRequestTestWithInvalidDisplayData() throws Exception {

            ObjectMapper mapper = new ObjectMapper();
            CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest("bevis"), CredentialConfigurationRequestResource.class);
            input.credentialMetadata().claims().getFirst().display().clear(); // remove all display entries to make it invalid
            mockMvc.perform(post("/v1/credential-configuration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("validation_error"));
        }

        @DisplayName("and create credential-configuration without exampleData then the response is 201 with CredentialConfiguration as body")
        @Test
        void postRequestTestWithEmptyExampleData() throws Exception {

            ObjectMapper mapper = new ObjectMapper();
            CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest("bevis"), CredentialConfigurationRequestResource.class);
            input.exampleCredentialData().clear(); // remove all display entries to make it invalid
            mockMvc.perform(post("/v1/credential-configuration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.vct").value(VCT_PREFIX + input.vct()))
                    .andExpect(jsonPath("$.example_credential_data").isEmpty());
        }

        @DisplayName("and create credential-configuration with exampleData with invalid data then the response is 400 with errormessage as body")
        @Test
        void postRequestTestWithInvalidExampleData() throws Exception {

            ObjectMapper mapper = new ObjectMapper();
            String invalidExampleCredentialMetadata = """
                    "example_credential_data": [{
                        "value": "value1"
                    }]""";
            CredentialConfiguration input = mapper.readValue(getPostRequestWithExampleData("bevis", invalidExampleCredentialMetadata), CredentialConfiguration.class);
            mockMvc.perform(post("/v1/credential-configuration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("validation_error"));
        }

        @Nested
        @DisplayName("when calling PUT to credential-configuration endpoint")
        class TestUpdate {
            @DisplayName("and update credential-configuration with valid input then the response is 200 with updated CredentialConfiguration as body")
            @Test
            void putUpdateBevisTypeOk() throws Exception {

                ObjectMapper mapper = new ObjectMapper();
                String vct = "net.eidas2sandkasse:bevisetmitt";
                String credentialConfigurationId = "cc-id";
                CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest(vct), CredentialConfigurationRequestResource.class);
                CredentialConfiguration output = mapper.readValue(getPostResponse(vct, credentialConfigurationId), CredentialConfiguration.class);


                when(redisService.getBevisType(eq(vct))).thenReturn(createCredentialConfigurationData(credentialConfigurationId, vct));

                mockMvc.perform(put("/v1/credential-configuration")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(input)))
                        .andExpect(status().isOk())
                        .andExpect(content().json(mapper.writeValueAsString(output)));

                verify(redisService).updateBevisType(any(CredentialConfigurationData.class));
            }

            @DisplayName("and update credential-configuration with invalid input then the response is 400 with error message body")
            @Test
            void putUpdateBevisTypeInvalidInput() throws Exception {

                ObjectMapper mapper = new ObjectMapper();
                String vct = "net.eidas2sandkasse:bevisetmitt";
                String credentialConfigurationId = "cc-id";
                CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest(vct), CredentialConfigurationRequestResource.class);
                input.credentialMetadata().display().clear(); // remove all display entries to make it invalid (bevis name).

                when(redisService.getBevisType(eq(vct))).thenReturn(createCredentialConfigurationData(credentialConfigurationId, vct));

                mockMvc.perform(put("/v1/credential-configuration")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(input)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("validation_error"));

                verify(redisService, never()).updateBevisType(any(CredentialConfigurationData.class));
            }

            @DisplayName("and update credential-configuration that does not exist then the response is 400 with invalid_request error body")
            @Test
            void putUpdateBevisTypeWhenNotExists() throws Exception {

                ObjectMapper mapper = new ObjectMapper();
                String vct = "net.eidas2sandkasse:bevisetmitt";
                CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest(vct), CredentialConfigurationRequestResource.class);


                when(redisService.getBevisType(eq(vct))).thenReturn(null);

                mockMvc.perform(put("/v1/credential-configuration")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(input)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("invalid_request"));

                verify(redisService, never()).updateBevisType(any(CredentialConfigurationData.class));
            }
        }

        private String getPostRequest(String vct) {
            return getPostRequestWithExampleData(vct, getExampleCredentialMetadata());
        }

        private String getPostRequestWithExampleData(String vct, String exampleCredentialMetadata) {

            return
                    """ 
                            {
                                "vct": "%s",
                                "format": "dc+sd-jwt",
                                %s,
                                %s
                            }
                            """.formatted(vct, exampleCredentialMetadata, getCredentialMetadata());
        }

        private String getPostResponse(String vct, String credentialConfigurationId) {
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

        private String getPostResponse(String vctName) {
            String credentialConfigurationId = "net.eidas2sandkasse:" + vctName + "_sd_jwt_vc";
            String vct = "net.eidas2sandkasse:" + vctName;
            return getPostResponse(vct, credentialConfigurationId);
        }

        private String getExampleCredentialMetadata() {
            return """
                    "example_credential_data": [{
                        "name": "claim1",
                        "value": "value1"
                    }]""";
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
            CredentialConfigurationData input1 = new CredentialConfigurationData(createCredentialConfiguration("example1_sd-jwt", "example1"));
            CredentialConfigurationData input2 = new CredentialConfigurationData(createCredentialConfiguration("example2_sd-jwt", "example2"));
            CredentialConfigurationData input3 = new CredentialConfigurationData(createCredentialConfiguration("example3_sd-jwt", "example3"));
            when(redisService.getAll()).thenReturn(List.of(input1, input2, input3));
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

        @DisplayName("search with credential_configuration_id as request param should return 404 when the credentialConfigurationId is not found")
        @Test
        void searchByCredentialConfigurationIdWhenIdDoesNotExistTest() throws Exception {
            mockMvc.perform(get("/v1/credential-configuration/search").param("credentialConfigurationId", "nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }

    private static CredentialConfiguration createCredentialConfiguration(String credentialConfigurationId, String vct) {
        return new CredentialConfiguration(credentialConfigurationId, vct, "dc+sd-jwt", List.of(new ExampleCredentialData("bar", "val")), new CredentialMetadata(new ArrayList<>(), new ArrayList<>()));
    }

    @Nested
    @DisplayName("when calling DELETE to credential-configuration endpoint")
    class TestDelete {

        @DisplayName("delete with vct as request param should return 204 when vct is found and deleted")
        @Test
        public void testDelete() throws Exception {
            String vct = "my-vct";
            when(redisService.getBevisType(eq(vct))).thenReturn(createCredentialConfigurationData("cc-id", vct));
            mockMvc.perform(delete("/v1/credential-configuration").param("vct", vct))
                    .andExpect(status().isNoContent());
            verify(redisService).delete(eq(vct));
        }

        @DisplayName("deleteAll should return 204 when all is found and deleted")
        @Test
        public void testDeleteAll() throws Exception {
            when(redisService.getAll()).thenReturn(List.of(createCredentialConfigurationData("cc-id", "vct_1")));
            mockMvc.perform(delete("/v1/credential-configuration/all").header("X-API-KEY", "test-api-key"))
                    .andExpect(status().isNoContent());
            verify(redisService).deleteAll();
        }


    }

    private static CredentialConfigurationData createCredentialConfigurationData(String credentialConfigurationId, String vct) {
        return new CredentialConfigurationData(credentialConfigurationId, vct, "dc+sd-jwt", List.of(new ExampleCredentialDataData("bar", "val")), new CredentialMetadataData(new ArrayList<>(), new ArrayList<>()));
    }
}
