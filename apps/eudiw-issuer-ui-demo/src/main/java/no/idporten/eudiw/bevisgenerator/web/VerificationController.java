package no.idporten.eudiw.bevisgenerator.web;

import jakarta.validation.Valid;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.web.models.StartVerificationForm;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

@Controller
public class VerificationController {

    private final IssuerServerService issuerServerService;
    private final IssuerServerProperties properties;

    public VerificationController(IssuerServerService issuerServerService, IssuerServerProperties properties) {
        this.issuerServerService = issuerServerService;
        this.properties = properties;
    }

    @ModelAttribute("issuerUrl")
    public String issuerUrl() {
        return properties.credentialIssuer();
    }

    @GetMapping("/verification-start")
    public ModelAndView verify() {
        return baseView(new StartVerificationForm());
    }

    @PostMapping("/verification-start")
    public ModelAndView startVerification(@Valid @ModelAttribute("verificationForm") StartVerificationForm form,
                                          BindingResult bindingResult) {
        ModelAndView view = baseView(form);
        if (bindingResult.hasErrors()) {
            return view;
        }
        return view.addObject("verificationSuccessMessage", "DCQL query er registrert.");
    }

    private ModelAndView baseView(StartVerificationForm form) {
        List<CredentialConfiguration> credentialConfigurations = new ArrayList<>(issuerServerService.getAll());
        credentialConfigurations.addAll(issuerServerService.getAllSubjectCredentialConfigurations());
        return new ModelAndView("verification-start")
                .addObject("verificationForm", form)
                .addObject("credentialConfigurations", credentialConfigurations);
    }
}
