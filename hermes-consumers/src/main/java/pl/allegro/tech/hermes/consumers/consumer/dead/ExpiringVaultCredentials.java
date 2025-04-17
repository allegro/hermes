package pl.allegro.tech.hermes.consumers.consumer.dead;

import com.google.auth.oauth2.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.bigdata.hadoop.vaultgcpjava.VaultGCPAuthCredentials;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;

public class ExpiringVaultCredentials extends VaultGCPAuthCredentials {

    private static final Logger logger = LoggerFactory.getLogger(ExpiringVaultCredentials.class);
    private static final long serialVersionUID = 4556936364828217688L;
    private final long tokenLifetimeMillis;


    public ExpiringVaultCredentials(String vaultUrl, String vaultServiceAccount, String vaultMasterToken,
                                    long tokenLifetimeMillis) throws GeneralSecurityException {
        super(vaultUrl, vaultServiceAccount, vaultMasterToken);
        this.tokenLifetimeMillis = tokenLifetimeMillis;
    }

    @Override
    public AccessToken refreshAccessToken() throws IOException {
        logger.info("Refreshing access token");
        AccessToken accessToken = super.refreshAccessToken();
        long aboutHourExpirationTime = Instant.now().plusMillis(tokenLifetimeMillis).toEpochMilli();
        long expirationTime = Math.min(accessToken.getExpirationTime().getTime(), aboutHourExpirationTime);
        logger.info("Access token refreshed, new expiration time: " + new Date(expirationTime));
        return new AccessToken(accessToken.getTokenValue(), new Date(expirationTime));

    }
}