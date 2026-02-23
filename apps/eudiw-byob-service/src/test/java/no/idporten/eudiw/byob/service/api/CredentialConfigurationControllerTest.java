package no.idporten.eudiw.byob.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.byob.service.data.RedisService;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialMetadata;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;
import no.idporten.eudiw.byob.service.model.data.CredentialConfigurationData;
import no.idporten.eudiw.byob.service.model.data.CredentialMetadataData;
import no.idporten.eudiw.byob.service.model.data.ExampleCredentialDataData;
import no.idporten.eudiw.byob.service.model.web.CredentialConfigurationRequestResource;
import no.idporten.eudiw.byob.service.service.CredentialConfigurationContext;
import no.idporten.eudiw.byob.service.service.CredentialConfigurationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.byob.service.service.CredentialConfigurationContext.PUBLIC_CREDENTIAL_TYPE_PREFIX;

import static org.hamcrest.Matchers.hasSize;
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
    @DisplayName("when calling POST to credential-configurations endpoint")
    class TestCreate {

        @DisplayName("and create credential-configuration with valid input then the response is 201 with created CredentialConfiguration as body")
        @Test
        void postRequestTest() throws Exception {

            ObjectMapper mapper = new ObjectMapper();
            CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest("bevisetmitt"), CredentialConfigurationRequestResource.class);
            CredentialConfiguration output = mapper.readValue(getPostResponse("bevisetmitt"), CredentialConfiguration.class);
            mockMvc.perform(post("/v1/public/credential-configurations")
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
            mockMvc.perform(post("/v1/public/credential-configurations")
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
            mockMvc.perform(post("/v1/public/credential-configurations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(input)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.credential_type").value(CredentialConfigurationContext.PUBLIC_CREDENTIAL_TYPE_PREFIX + input.credentialType()))
                    .andExpect(jsonPath("$.example_credential_data").isEmpty());
        }

        @Nested
        @DisplayName("when calling PUT to credential-configurations endpoint")
        class TestUpdate {
            @DisplayName("and update credential-configuration with valid input then the response is 200 with updated CredentialConfiguration as body")
            @Test
            void putUpdateBevisTypeOk() throws Exception {

                ObjectMapper mapper = new ObjectMapper();
                String credentialType = "net.eidas2sandkasse:bevisetmitt";
                String credentialConfigurationId = "cc-id";
                CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest(credentialType), CredentialConfigurationRequestResource.class);
                CredentialConfiguration output = mapper.readValue(getPostResponse(credentialType, credentialConfigurationId), CredentialConfiguration.class);


                when(redisService.getBevisType(eq(credentialType))).thenReturn(createCredentialConfigurationData(credentialConfigurationId, credentialType));

                mockMvc.perform(put("/v1/public/credential-configurations")
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
                String credentialType = "net.eidas2sandkasse:bevisetmitt";
                String credentialConfigurationId = "cc-id";
                CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest(credentialType), CredentialConfigurationRequestResource.class);
                input.credentialMetadata().display().clear(); // remove all display entries to make it invalid (bevis name).

                when(redisService.getBevisType(eq(credentialType))).thenReturn(createCredentialConfigurationData(credentialConfigurationId, credentialType));

                mockMvc.perform(put("/v1/public/credential-configurations")
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
                String credentialType = "net.eidas2sandkasse:bevisetmitt";
                CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest(credentialType), CredentialConfigurationRequestResource.class);


                when(redisService.getBevisType(eq(credentialType))).thenReturn(null);

                mockMvc.perform(put("/v1/public/credential-configurations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(input)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("invalid_request"));

                verify(redisService, never()).updateBevisType(any(CredentialConfigurationData.class));
            }

            @DisplayName("and update non-public credential-configuration from public API gives 403 with error response")
            @Test
            void putUpdateNonPublicBevisNotAllowedInPublicApi() throws Exception {
                ObjectMapper mapper = new ObjectMapper();
                String credentialType = "private.niot.public:bevisetmitt";
                String credentialConfigurationId = "cc-id";
                CredentialConfigurationRequestResource input = mapper.readValue(getPostRequest(credentialType), CredentialConfigurationRequestResource.class);
                CredentialConfiguration output = mapper.readValue(getPostResponse(credentialType, credentialConfigurationId), CredentialConfiguration.class);
                when(redisService.getBevisType(eq(credentialType))).thenReturn(createCredentialConfigurationData(credentialConfigurationId, credentialType));
                mockMvc.perform(put("/v1/public/credential-configurations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(input)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.error").value("invalid_request"));
                verifyNoMoreInteractions(redisService);
            }

        }

        private String getPostRequest(String credentialType) {
            return getPostRequestWithExampleData(credentialType, getExampleCredentialMetadata());
        }

        private String getPostRequestWithExampleData(String credentialType, String exampleCredentialMetadata) {

            return
                    """ 
                            {
                                "credential_type": "%s",
                                "format": "dc+sd-jwt",
                                "scope": "eudiw:junit",
                                %s,
                                %s
                            }
                            """.formatted(credentialType, exampleCredentialMetadata, getCredentialMetadata());
        }

        private String getPostResponse(String credentialType, String credentialConfigurationId) {
            return
                    """ 
                            {
                                "credential_configuration_id": "%s",
                                "credential_type": "%s",
                                "format": "dc+sd-jwt",
                                "scope": "eudiw:junit",
                                %s,
                                %s
                            }
                            """.formatted(credentialConfigurationId, credentialType, getExampleCredentialMetadata(), getCredentialMetadata());
        }

        private String getPostResponse(String vctName) {
            String credentialConfigurationId = "net.eidas2sandkasse:" + vctName + "_sd_jwt_vc";
            String vct = "net.eidas2sandkasse:" + vctName;
            return getPostResponse(vct, credentialConfigurationId);
        }

        private String getExampleCredentialMetadata() {
            return """
                    "example_credential_data": {
                        "claim1": "value1"
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
    @DisplayName("when calling GET to credential-configurations public endpoint")
    class TestGetAllForPublicEdit {

        @DisplayName("to get all credential-configurations for public then return all registered entries that can be edited by all with response 200")
        @Test
        void getAllRequestTest() throws Exception {
            CredentialConfigurationData input1 = new CredentialConfigurationData(createCredentialConfiguration("net.eidas2sandkasse:example1_sd-jwt", "net.eidas2sandkasse:example1"));
            CredentialConfigurationData input2 = new CredentialConfigurationData(createCredentialConfiguration("net.eidas2sandkasse:example2_sd-jwt", "net.eidas2sandkasse:example2"));
            CredentialConfigurationData input3 = new CredentialConfigurationData(createCredentialConfiguration("example3_sd-jwt", "example3"));
            when(redisService.getAll()).thenReturn(List.of(input1, input2, input3));
            mockMvc.perform(get("/v1/public/credential-configurations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credential_configurations", hasSize(2)))
                    .andExpect(jsonPath("$.credential_configurations[0].credential_configuration_id").value("net.eidas2sandkasse:example1_sd-jwt"))
                    .andExpect(jsonPath("$.credential_configurations[1].credential_configuration_id").value("net.eidas2sandkasse:example2_sd-jwt"));
        }
    }

    @Nested
    @DisplayName("when calling GET to credential-configurations admin endpoint")
    class TestGetAllForIssue {

        @DisplayName("to get all credential-configurations for edit then return all registered entries with response 200")
        @Test
        void getAllRequestTest() throws Exception {
            CredentialConfigurationData input1 = new CredentialConfigurationData(createCredentialConfiguration("net.eidas2sandkasse:example1_sd-jwt", "net.eidas2sandkasse:example1"));
            CredentialConfigurationData input2 = new CredentialConfigurationData(createCredentialConfiguration("net.eidas2sandkasse:example2_sd-jwt", "net.eidas2sandkasse:example2"));
            CredentialConfigurationData input3 = new CredentialConfigurationData(createCredentialConfiguration("example3_sd-jwt", "example3"));
            when(redisService.getAll()).thenReturn(List.of(input1, input2, input3));
            mockMvc.perform(get("/v1/admin/credential-configurations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credential_configurations", hasSize(3)))
                    .andExpect(jsonPath("$.credential_configurations[0].credential_configuration_id").value("net.eidas2sandkasse:example1_sd-jwt"))
                    .andExpect(jsonPath("$.credential_configurations[1].credential_configuration_id").value("net.eidas2sandkasse:example2_sd-jwt"))
                    .andExpect(jsonPath("$.credential_configurations[2].credential_configuration_id").value("example3_sd-jwt"));
        }
    }

    @Nested
    @DisplayName("when calling GET to credential-configurations endpoint")
    class TestGetSingle {

        @DisplayName("with credential_type as path is should return response 200 with valid credentialConfiguration when the credential_type is found")
        @Test
        void getByIdWhenIdDoesExistTest() throws Exception {
            String credentialType = "example_sd-jwt";
            String credentialConfigurationId = PUBLIC_CREDENTIAL_TYPE_PREFIX + credentialType;
            when(service.getCredentialConfiguration(eq(credentialType), any())).thenReturn(createCredentialConfiguration(credentialConfigurationId, credentialType));
            mockMvc.perform(get("/v1/public/credential-configurations/{id}", credentialType))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credential_configuration_id").value(credentialConfigurationId))
                    .andExpect(jsonPath("$.credential_type").value(credentialType));
        }

        @DisplayName("with credential_type as path is should return response 404 when the id is not found")
        @Test
        void getByIdWhenIdDoesNotExistTest() throws Exception {
            mockMvc.perform(get("/v1/public/credential-configurations/{id}", "nonexistent"))
                    .andExpect(status().isNotFound());
        }

        @DisplayName("with search and credential_configuration_id as request parameter should return response 200 with valid credentialConfiguration when the credential_configuration_id is found")
        @Test
        void searchByCredentialConfigurationIdWhenIdDoesExistTest() throws Exception {
            String credentialType = "example_sd-jwt";
            String credentialConfigurationId = PUBLIC_CREDENTIAL_TYPE_PREFIX + credentialType;
            when(service.searchCredentialConfiguration(eq(credentialConfigurationId))).thenReturn(createCredentialConfiguration(credentialConfigurationId, credentialType));
            mockMvc.perform(get("/v1/public/credential-configurations/search").param("credentialConfigurationId", credentialConfigurationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credential_configuration_id").value(credentialConfigurationId))
                    .andExpect(jsonPath("$.credential_type").value(credentialType));
        }

        @DisplayName("search with credential_configuration_id as request param should return 404 when the credentialConfigurationId is not found")
        @Test
        void searchByCredentialConfigurationIdWhenIdDoesNotExistTest() throws Exception {
            mockMvc.perform(get("/v1/public/credential-configurations/search").param("credentialConfigurationId", "nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }

    private static CredentialConfiguration createCredentialConfiguration(String credentialConfigurationId, String credentialType) {
        return new CredentialConfiguration(credentialConfigurationId, credentialType, "dc+sd-jwt", "eudiw:foo", new ExampleCredentialData(Map.of("bar", "val")), new CredentialMetadata(new ArrayList<>(), new ArrayList<>()));
    }

    @Nested
    @DisplayName("when calling DELETE to credential-configurations endpoint")
    class TestDelete {

        @DisplayName("delete with credential_type as request param should return 204 when credential type is found and deleted")
        @Test
        public void testDelete() throws Exception {
            String credentialType = "net.eidas2sandkasse:my-vct";
            when(redisService.getBevisType(eq(credentialType))).thenReturn(createCredentialConfigurationData("cc-id", credentialType));
            mockMvc.perform(delete("/v1/public/credential-configurations").param("credential-type", credentialType))
                    .andExpect(status().isNoContent());
            verify(redisService).delete(eq(credentialType));
        }

        @DisplayName("delete a non-public credential type through public API should return 403 with error response and not delete the credential type")
        @Test
        public void testDeleteNonPublicCredentialTypeThroughPublicAPIIsNotAllowed() throws Exception {
            String credentialType = "my-vct";
            when(redisService.getBevisType(eq(credentialType))).thenReturn(createCredentialConfigurationData("cc-id", credentialType));
            mockMvc.perform(delete("/v1/public/credential-configurations").param("credential-type", credentialType))
                    .andExpect(status().isForbidden());
            verifyNoMoreInteractions(redisService);
        }


        @DisplayName("deleteAll should return 204 when all is found and deleted")
        @Test
        public void testDeleteAll() throws Exception {
            when(redisService.getAll()).thenReturn(List.of(createCredentialConfigurationData("cc-id", "vct_1")));
            mockMvc.perform(delete("/v1/admin/credential-configurations/all").header("X-API-KEY", "test-api-key"))
                    .andExpect(status().isNoContent());
            verify(redisService).deleteAll();
        }

    }

    private static CredentialConfigurationData createCredentialConfigurationData(String credentialConfigurationId, String credentialType) {
        return new CredentialConfigurationData(credentialConfigurationId, credentialType, "dc+sd-jwt", "eudiw:foo", new ExampleCredentialDataData(Map.of("bar", "val")), new CredentialMetadataData(new ArrayList<>(), new ArrayList<>()));
    }
}
