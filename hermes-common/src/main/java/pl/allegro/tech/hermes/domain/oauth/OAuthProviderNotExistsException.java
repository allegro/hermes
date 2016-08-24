package pl.allegro.tech.hermes.domain.oauth;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class OAuthProviderNotExistsException extends HermesException {

    public OAuthProviderNotExistsException(String oAuthProviderName) {
        super(String.format("OAuth provider %s does not exist", oAuthProviderName));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OAUTH_PROVIDER_NOT_EXISTS;
    }
}
