package no.idporten.eudiw.bevisgenerator.web;

import jakarta.validation.Valid;
import no.idporten.eudiw.bevisgenerator.exception.IssuerServerException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
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
        return baseView(new RevokeForm());
    }

    @PostMapping("/revoke")
    public ModelAndView revoke(@Valid @ModelAttribute("revokeForm") RevokeForm revokeForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return baseView(revokeForm);
        }

        CredentialConfiguration credentialConfiguration = issuerServerService.getById(revokeForm.credentialConfigurationId());
        if (credentialConfiguration == null) {
            bindingResult.rejectValue("credentialConfigurationId", "notFound", "Credential configuration finnes ikkje");
            return baseView(revokeForm);
        }

        try {
            issuerServerService.revokeCredential(credentialConfiguration, revokeForm.issuanceTransactionId());
        } catch (IssuerServerException e) {
            logger.error("Failed to revoke credential", e);
            return baseView(revokeForm).addObject("errorMessage", e.getCauseMessage());
        }

        return baseView(new RevokeForm())
                .addObject("successMessage", "Beviset blei revokert dersom det eksisterte.");
    }

    private ModelAndView baseView(RevokeForm revokeForm) {
        return new ModelAndView("revoke")
                .addObject("revokeForm", revokeForm)
                .addObject("credentialConfigurations", issuerServerService.getAll());
    }
}
