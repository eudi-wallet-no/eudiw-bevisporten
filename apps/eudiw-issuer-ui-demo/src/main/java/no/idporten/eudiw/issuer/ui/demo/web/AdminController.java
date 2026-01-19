package no.idporten.eudiw.issuer.ui.demo.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import no.idporten.eudiw.issuer.ui.demo.credentials.CredentialDto;
import no.idporten.eudiw.issuer.ui.demo.credentials.CredentialService;
import no.idporten.eudiw.issuer.ui.demo.web.models.AddCredentialForm;
import no.idporten.eudiw.issuer.ui.demo.web.models.EditCredentialForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AdminController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CredentialService credentialService;

    public AdminController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }


    @GetMapping("/admin")
    public ModelAndView admin() {
        return new ModelAndView("admin", "credentials", credentialService.getCredentials());
    }

    @GetMapping("/add-credential")
    public ModelAndView addCredential() {
        CredentialDto emptyDto = credentialService.getEmptyCredentialDefinition();
        return new ModelAndView("add", "addCredentialForm", new AddCredentialForm(emptyDto.json()));
    }

    @PostMapping("/add-credential")
    public ModelAndView addNewCredential(@Valid AddCredentialForm addCredentialForm, BindingResult bindingResult) throws JsonProcessingException {
        if (bindingResult.hasErrors()) {
            // TODO: Add json validation
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return new ModelAndView("add", "addCredentialForm", addCredentialForm);
        }

        credentialService.storeCredential(new CredentialDto(addCredentialForm.vct(), addCredentialForm.json()));

        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentials());
    }

    @GetMapping("/edit-credential/{credential_configuration_id}")
    public ModelAndView editCredential(@PathVariable("credential_configuration_id") String credentialConfigurationId) {
        CredentialDto cd = credentialService.findCredential(credentialConfigurationId);
        return new ModelAndView("edit", "editCredentialForm", new EditCredentialForm(credentialConfigurationId, cd.json()));
    }

    @PostMapping("/edit-credential/{credential_configuration_id}")
    public ModelAndView editCredentialPost(
            @PathVariable("credential_configuration_id") String credentialConfigurationId,
            @Valid EditCredentialForm editCredentialForm,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            // TODO: Add json validation
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return new ModelAndView("add", "editCredentialForm", editCredentialForm);
        }

        logger.info("Editing credential with id {}", credentialConfigurationId);

        credentialService.editCredential(new CredentialDto(credentialConfigurationId, editCredentialForm.json()));
        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentials());
    }


    @GetMapping("/delete-credential/{credential_configuration_id}")
    public ModelAndView deleteCredential(@PathVariable("credential_configuration_id") String credentialConfigurationId) {
        logger.info("Deleting credential with id {}", credentialConfigurationId);

        credentialService.deleteCredential(credentialConfigurationId);
        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentials());
    }
}
