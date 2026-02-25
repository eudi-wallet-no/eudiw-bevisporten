package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClaimsSourceService implements InitializingBean {

    private final List<ClaimsSource> claimsSources;

    public ClaimsSource findClaimsSource(URI uri) {
        if (! "class".equals(uri.getScheme())) {
            throw new IssuerServerException("server_error", "Unsupported claims source URI scheme for uri [%s]".formatted(uri), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return claimsSources.stream()
                .filter(claimsSource -> claimsSource.getClass().getName().equals(uri.getAuthority()))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("server_error", "Unknown claims source for uri [%s]".formatted(uri), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Claims source service managing {} claims sources", claimsSources.size());
        for (ClaimsSource claimsSource : claimsSources) {
            log.info("Registered claims source: {}", claimsSource.getClass().getName());
        }
    }

}
