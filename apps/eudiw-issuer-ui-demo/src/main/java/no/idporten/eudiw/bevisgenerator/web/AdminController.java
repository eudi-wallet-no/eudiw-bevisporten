package no.idporten.eudiw.bevisgenerator.web;

import jakarta.validation.Valid;
import no.idporten.eudiw.bevisgenerator.web.models.CredentialDto;
import no.idporten.eudiw.bevisgenerator.byob.CredentialService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.web.models.AddCredentialForm;
import no.idporten.eudiw.bevisgenerator.web.models.EditCredentialForm;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.CreateForm;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.EditForm;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.SimpleCredentialForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AdminController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CredentialService credentialService;
    private final IssuerServerProperties properties;

    public AdminController(CredentialService credentialService, IssuerServerProperties properties) {
        this.credentialService = credentialService;
        this.properties = properties;
    }

    @ModelAttribute("issuerUrl")
    public String issuerUrl() {
        return properties.credentialIssuer();
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
    public ModelAndView addNewCredential(@Valid AddCredentialForm addCredentialForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // TODO: Add json validation
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return new ModelAndView("add", "addCredentialForm", addCredentialForm);
        }

        credentialService.storeCredential(new CredentialDto(addCredentialForm.vct(), addCredentialForm.json()));

        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentials());
    }

    @GetMapping("/edit-credential/{vct}")
    public ModelAndView editCredential(@PathVariable("vct") String vct) {
        CredentialDto cd = credentialService.findCredential(vct);
        return new ModelAndView("edit", "editCredentialForm", new EditCredentialForm(vct, cd.json()));
    }

    @PostMapping("/edit-credential/{vct}")
    public ModelAndView editCredentialPost(
            @PathVariable("vct") String vct,
            @Valid EditCredentialForm editCredentialForm,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            // TODO: Add json validation
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return new ModelAndView("add", "editCredentialForm", editCredentialForm);
        }

        logger.info("Editing credential with vct {}", vct);

        credentialService.editCredential(new CredentialDto(vct, editCredentialForm.json()));
        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentials());
    }

    /*
    @GetMapping("/delete-credential/{credential_configuration_id}")
    public ModelAndView deleteCredential(@PathVariable("credential_configuration_id") String credentialConfigurationId) {
        logger.info("Deleting credential with vct {}", credentialConfigurationId);

        credentialService.deleteCredential(credentialConfigurationId);
        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentials());
    }*/


    @GetMapping("/add-credential-new")
    public ModelAndView showForm() {
        return new ModelAndView("add-new", "form", new SimpleCredentialForm());
    }

    @PostMapping("/add-credential-new")
    public ModelAndView submitForm(@Validated(CreateForm.class) @Valid @ModelAttribute("form") SimpleCredentialForm form,
                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return new ModelAndView("add-new", "form", form);
        }

        credentialService.storeCredential(form);
        return new ModelAndView("redirect:/admin");
    }

    @GetMapping("/edit-credential-new/{vct}")
    public ModelAndView edit(@PathVariable String vct) {
        SimpleCredentialForm form = credentialService.findSimpleCredential(vct);
        return new ModelAndView("edit-new", "form", form);
    }

    @PostMapping("/edit-credential-new/{vct}")
    public ModelAndView edit(@PathVariable String vct,
                             @Validated(EditForm.class) @Valid SimpleCredentialForm form,
                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return new ModelAndView("edit-new", "form", form);
        }

        credentialService.editCredential(new SimpleCredentialForm(vct, form.name(), form.claims()));
        return new ModelAndView("redirect:/admin");
    }

}
