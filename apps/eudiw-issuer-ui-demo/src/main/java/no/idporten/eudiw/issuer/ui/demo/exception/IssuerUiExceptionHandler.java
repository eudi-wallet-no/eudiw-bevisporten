package no.idporten.eudiw.issuer.ui.demo.exception;

import no.idporten.eudiw.issuer.ui.demo.issuer.config.IssuerServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class IssuerUiExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(IssuerUiExceptionHandler.class);

    private final IssuerServerProperties properties;

    @Autowired
    public IssuerUiExceptionHandler(IssuerServerProperties properties) {
        this.properties = properties;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFoundException(
            @SuppressWarnings("unused") NoResourceFoundException _unused) {
        return getModelAndView("error/404");
    }

    private ModelAndView getModelAndView(String viewName) {
        return new ModelAndView(viewName).addObject("issuerUrl", properties.credentialIssuer());
    }

    @ExceptionHandler(IssuerServerException.class)
    public ModelAndView handleIssuerServerException(IssuerServerException e) {
        log.error("Unexpected exception from issuer-server", e);
        return getModelAndView("error/error").addObject("errorMessage", e.getMessage()).addObject("statusCode", e.getHttpStatusCode()).addObject("details", e.getCauseMessage());
    }

    @ExceptionHandler(IssuerUiException.class)
    public ModelAndView handleIssuerUiException(IssuerUiException e) {
        log.error("IssuerUiException", e);
        return getModelAndView("error/error").addObject("errorMessage", e.getMessage());
    }

}
