package no.idporten.eudiw.bevisgenerator.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateAccessibilityContractTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "admin.html",
            "add.html",
            "add-new.html",
            "edit.html",
            "edit-new.html",
            "index.html",
            "issuance-complete.html",
            "issue.html",
            "issuer_response.html",
            "revoke.html",
            "start.html",
            "verification-presentation.html",
            "verification-result.html",
            "verification-start.html",
            "error/404.html",
            "error/error.html"
    })
    void pageHasDescriptiveTitleAndFocusableMainContent(String template) throws IOException {
        String html = new ClassPathResource("templates/" + template)
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(html)
                .contains("<title")
                .doesNotContain("<title>Bevisgenerator</title>")
                .contains("<main id=\"main-content\" tabindex=\"-1\">");
    }
}
