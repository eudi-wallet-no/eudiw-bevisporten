package no.idporten.eudiw.oauth2.server;

import lombok.Getter;

@Getter
public class UseAttestationChallengeOAuth2Exception extends OAuth2Exception {

    public static final String OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER = "OAuth-Client-Attestation-Challenge";

    private final String attestationChallenge;

    public UseAttestationChallengeOAuth2Exception(String errorDescription, String attestationChallenge) {
        super(USE_ATTESTATION_CHALLENGE, errorDescription, 401);
        this.attestationChallenge = attestationChallenge;
    }

}
