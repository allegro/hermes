package pl.allegro.tech.hermes.consumers.consumer.dead;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class CachingVaultCredentialsProvider implements CredentialsProvider {

    private final String vaultUrl;
    private final String vaultMasterToken;
    private final String vaultServiceAccount;
    private final long tokenLifetimeMillis;

    private Credentials cachedCredentials;

    private CachingVaultCredentialsProvider(Builder builder) {
        vaultUrl = builder.vaultUrl;
        vaultMasterToken = builder.vaultMasterToken;
        vaultServiceAccount = builder.vaultServiceAccount;
        tokenLifetimeMillis = builder.tokenLifetimeMillis;
    }

    @Override
    public synchronized Credentials getCredentials() throws IOException {
        try {
            if (cachedCredentials == null) {
                cachedCredentials = new ExpiringVaultCredentials(vaultUrl, vaultServiceAccount, vaultMasterToken, tokenLifetimeMillis);
            }
            return cachedCredentials;
        } catch (GeneralSecurityException e) {
            throw new IOException("Could not create ExpiringVaultCredentials object", e);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String vaultUrl;
        private String vaultMasterToken;
        private String vaultServiceAccount;
        private long tokenLifetimeMillis;

        public Builder withVaultUrl(String vaultUrl) {
            this.vaultUrl = vaultUrl;
            return this;
        }

        public Builder withVaultMasterToken(String vaultMasterToken) {
            this.vaultMasterToken = vaultMasterToken;
            return this;
        }

        public Builder withVaultServiceAccount(String vaultServiceAccount) {
            this.vaultServiceAccount = vaultServiceAccount;
            return this;
        }

        public Builder withTokenLifetimeMillis(long tokenLifetimeMillis) {
            this.tokenLifetimeMillis = tokenLifetimeMillis;
            return this;
        }

        public CachingVaultCredentialsProvider build() {
            return new CachingVaultCredentialsProvider(this);
        }
    }
}
