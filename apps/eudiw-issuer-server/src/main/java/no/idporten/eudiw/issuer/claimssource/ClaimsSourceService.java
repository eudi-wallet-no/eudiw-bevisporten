package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClaimsSourceService implements InitializingBean {

    private final List<ClaimsSource> claimsSources;

    private static final List<String> SUPPORTED_SCHEMES = List.of("class", "http", "https");

    public ClaimsSource findClaimsSource(URI uri) {
        if (! SUPPORTED_SCHEMES.contains(uri.getScheme())) {
            throw new IssuerServerException("server_error", "Unsupported claims source URI scheme for uri [%s]".formatted(uri), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (uri.getScheme().startsWith("http")) {
            return findClaimsSourceByClassName(HttpPullPreAuthorizedClaimsSource.class.getName())
                    .orElseThrow(() -> new IssuerServerException("server_error", "Unknown claims source for uri [%s]".formatted(uri), HttpStatus.INTERNAL_SERVER_ERROR));
        }
        return findClaimsSourceByClassName(uri.getAuthority())
                .orElseThrow(() -> new IssuerServerException("server_error", "Unknown claims source for uri [%s]".formatted(uri), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private Optional<ClaimsSource> findClaimsSourceByClassName(String className) {
        return claimsSources.stream()
                .filter(claimsSource -> claimsSource.getClass().getName().equals(className))
                .findFirst();
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Claims source service managing {} claims sources", claimsSources.size());
        for (ClaimsSource claimsSource : claimsSources) {
            log.info("Registered claims source: {}", claimsSource.getClass().getName());
        }
    }

}
