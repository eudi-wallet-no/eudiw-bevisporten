package no.idporten.eudiw.bevisgenerator.web;

import jakarta.validation.Valid;
import no.idporten.eudiw.bevisgenerator.exception.IssuerServerException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.web.models.RevokeBySubjectForm;
import no.idporten.eudiw.bevisgenerator.web.models.RevokeForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class RevokeController {

    static final String TRANSACTION_ID_METHOD = "transaction-id";
    static final String PERSON_IDENTIFIER_METHOD = "person-identifier";

    private final Logger logger = LoggerFactory.getLogger(RevokeController.class);
    private final IssuerServerService issuerServerService;
    private final IssuerServerProperties properties;

    public RevokeController(IssuerServerService issuerServerService, IssuerServerProperties properties) {
        this.issuerServerService = issuerServerService;
        this.properties = properties;
    }

    @ModelAttribute("issuerUrl")
    public String issuerUrl() {
        return properties.credentialIssuer();
    }

    @GetMapping("/revoke")
    public ModelAndView revoke() {
        return baseView(new RevokeForm(), new RevokeBySubjectForm(), "");
    }

    @PostMapping("/revoke")
    public ModelAndView revoke(@Valid @ModelAttribute("revokeForm") RevokeForm revokeForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return baseView(revokeForm, new RevokeBySubjectForm(), TRANSACTION_ID_METHOD);
        }

        CredentialConfiguration credentialConfiguration = issuerServerService.getById(revokeForm.credentialConfigurationId());
        if (credentialConfiguration == null) {
            bindingResult.rejectValue("credentialConfigurationId", "notFound", "Bevistypen finst ikkje");
            return baseView(revokeForm, new RevokeBySubjectForm(), TRANSACTION_ID_METHOD);
        }

        int revokedCount;
        try {
            revokedCount = issuerServerService.revokeCredential(credentialConfiguration, revokeForm.issuanceTransactionId());
        } catch (IssuerServerException e) {
            logger.error("Failed to revoke credential", e);
            return baseView(revokeForm, new RevokeBySubjectForm(), TRANSACTION_ID_METHOD)
                    .addObject("txIdErrorMessage", e.getCauseMessage());
        }

        return resultView(revokedCount, credentialConfiguration.description())
                .addObject("processedIssuanceTransactionId", revokeForm.issuanceTransactionId());
    }

    @PostMapping("/revoke/by-subject")
    public ModelAndView revokeBySubject(@Valid @ModelAttribute("revokeBySubjectForm") RevokeBySubjectForm revokeBySubjectForm,
                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return baseView(new RevokeForm(), revokeBySubjectForm, PERSON_IDENTIFIER_METHOD);
        }
        CredentialConfiguration credentialConfiguration = issuerServerService.getSubjectCredentialConfigurationById(revokeBySubjectForm.credentialConfigurationId());
        if (credentialConfiguration == null) {
            return baseView(new RevokeForm(), revokeBySubjectForm, PERSON_IDENTIFIER_METHOD)
                    .addObject("subjectErrorMessage", "Bevistypen finst ikkje");
        }

        int revokedCount;
        try {
            revokedCount = issuerServerService.revokeCredentialBySubject(credentialConfiguration, revokeBySubjectForm.subjectIdentifier());
        } catch (IssuerServerException e) {
            logger.error("Failed to revoke credential by subject", e);
            return baseView(new RevokeForm(), revokeBySubjectForm, PERSON_IDENTIFIER_METHOD)
                    .addObject("subjectErrorMessage", e.getCauseMessage());
        }

        return resultView(revokedCount, credentialConfiguration.description())
                .addObject("processedCredentialConfigurationId", revokeBySubjectForm.credentialConfigurationId())
                .addObject("processedSubjectIdentifier", revokeBySubjectForm.subjectIdentifier());
    }

    private ModelAndView resultView(int revokedCount, String credentialDescription) {
        return new ModelAndView("revocation-result")
                .addObject("revokedCount", revokedCount)
                .addObject("credentialDescription", credentialDescription);
    }

    private ModelAndView baseView(
            RevokeForm revokeForm,
            RevokeBySubjectForm revokeBySubjectForm,
            String revocationMethod
    ) {
        return new ModelAndView("revoke")
                .addObject("revokeForm", revokeForm)
                .addObject("revokeBySubjectForm", revokeBySubjectForm)
                .addObject("revocationMethod", revocationMethod)
                .addObject("credentialConfigurations", issuerServerService.getAll())
                .addObject("subjectCredentialConfigurations", issuerServerService.getAllSubjectCredentialConfigurations());
    }
}
