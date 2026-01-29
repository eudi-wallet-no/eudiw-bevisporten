package no.idporten.eudiw.bevisgenerator.exception;

import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.lib.maskinporten.exception.MaskinportenClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(MaskinportenClientException.class)
    public ModelAndView handleMaskinportenClientException(MaskinportenClientException e) {
        log.error("Unexpected exception from Maskinporten", e);
        return getModelAndView("error/error").addObject("errorMessage", "Integration with Maskinporten failed").addObject("statusCode", HttpStatus.INTERNAL_SERVER_ERROR).addObject("details", e.getMessage());
    }

    @ExceptionHandler(ByobServiceException.class)
    public ModelAndView handleByobServiceException(ByobServiceException e) {
        log.error("Unexpected error from byob-service", e);
        return getModelAndView("error/error").addObject("errorMessage", "Failed to connect with byob-service").addObject("statusCode", e.getHttpStatusCode()).addObject("details", e.getMessage());
    }

    @ExceptionHandler(ByobServiceIOException.class)
    public ModelAndView handleByobServiceIOException(ByobServiceIOException e) {
        log.error("Failed to connect with byob-service", e);
        return getModelAndView("error/error").addObject("errorMessage", "Failed to connect with byob-service").addObject("statusCode", HttpStatus.SERVICE_UNAVAILABLE).addObject("details", e.getMessage());
    }
}
