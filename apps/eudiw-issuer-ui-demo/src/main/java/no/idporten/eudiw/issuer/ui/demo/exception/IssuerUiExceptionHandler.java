package no.idporten.eudiw.issuer.ui.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class IssuerUiExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(IssuerUiExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFoundException(
            @SuppressWarnings("unused") NoResourceFoundException _unused) {
        return new ModelAndView("error/404");
    }

    @ExceptionHandler(IssuerServerException.class)
    public ModelAndView handleIssuerServerException(IssuerServerException e) {
        log.error("Unexpected exception from issuer-server", e);
        return new ModelAndView("error/error").addObject("errorMessage", e.getMessage()).addObject("statusCode", e.getHttpStatusCode()).addObject("details", e.getCauseMessage());
    }

    @ExceptionHandler(IssuerUiException.class)
    public ModelAndView handleIssuerUiException(IssuerUiException e) {
        log.error("IssuerUiException", e);
        return new ModelAndView("error/error").addObject("errorMessage", e.getMessage());
    }

}
