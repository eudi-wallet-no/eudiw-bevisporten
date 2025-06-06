package no.idporten.sdk.oidcserver.protocol;

import no.idporten.sdk.oidcserver.util.StringUtils;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response data and formatting for form_post responses.
 */
public final class FormPostResponse extends ClientResponse {

    public FormPostResponse(String redirectUri, Map<String, String> parameters) {
        super(redirectUri, parameters);
    }

    /**
     * Suitable for form_post: Creates a HTML page with a JavaScript autosubmitting a form with redirect_uri and parameters.
     *
     * @return HTML with autosubmitting form
     */
    public String getRedirectForm() {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>");
        sb.append("<head><title>Submit This Form</title></head>");
        sb.append("<body onload=\"javascript:document.forms[0].submit()\">");
        sb.append("<form method=\"post\" action=\"" + getRedirectUri() + "\">");
        sb.append(getParameters().entrySet().stream()
                .filter(entry -> StringUtils.hasText(entry.getValue()))
                .map(entry -> "<input type=\"hidden\" name=\"%s\" value=\"%s\" />".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining()));
        sb.append("<noscript><input type=\"submit\" value=\"Click to redirect\"></noscript>");
        sb.append("</form>");
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }

    public String getContentType() {
        return "text/html;charset=UTF-8";
    }

    public Charset getCharacterEncoding() {
        return Charset.forName("UTF-8");
    }

}
