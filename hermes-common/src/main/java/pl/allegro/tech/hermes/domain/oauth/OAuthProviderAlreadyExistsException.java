package pl.allegro.tech.hermes.domain.oauth;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class OAuthProviderAlreadyExistsException extends HermesException {

    public OAuthProviderAlreadyExistsException(OAuthProvider oAuthProvider, Throwable cause) {
        super(String.format("OAuth Provider %s already exists", oAuthProvider.getName()), cause);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OAUTH_PROVIDER_ALREADY_EXISTS;
    }
}
