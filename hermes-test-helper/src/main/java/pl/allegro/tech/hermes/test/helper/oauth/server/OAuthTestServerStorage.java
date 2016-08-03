package pl.allegro.tech.hermes.test.helper.oauth.server;

import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.issuer.UUIDValueGenerator;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class OAuthTestServerStorage {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTestServerStorage.class);

    private final OAuthIssuer issuer;
    private final ConcurrentHashMap<String, String> clients;
    private final ConcurrentHashMap<String, String> owners;
    private final ConcurrentHashMap<String, List<String>> tokens;
    private final ConcurrentHashMap<String, AtomicInteger> accessCount;
    private final ConcurrentHashMap<String, AtomicInteger> tokenIssueCount;

    OAuthTestServerStorage() {
        issuer = new OAuthIssuerImpl(new UUIDValueGenerator());
        clients = new ConcurrentHashMap<>();
        owners = new ConcurrentHashMap<>();
        tokens = new ConcurrentHashMap<>();
        accessCount = new ConcurrentHashMap<>();
        tokenIssueCount = new ConcurrentHashMap<>();
    }

    void addClient(String clientId, String clientSecret) {
        clients.put(clientId, clientSecret);
    }

    void addResourceOwner(String username, String password) {
        owners.put(username, password);
    }

    boolean clientExists(String clientId, String clientSecret) {
        return clients.containsKey(clientId) && clients.get(clientId).equals(clientSecret);
    }

    boolean resourceOwnerExists(String username, String password) {
        return owners.containsKey(username) && owners.get(username).equals(password);
    }

    String issueToken(String owner) {
        try {
            String token = issuer.accessToken();
            addAccessToken(owner, token);
            int i = incrementTokenIssueCount(owner);
            logger.info("Token {} issued for user {}, count {}", token, owner, i);
            return token;
        } catch (OAuthSystemException e) {
            throw new RuntimeException(e);
        }
    }

    private int incrementTokenIssueCount(String owner) {
        if (tokenIssueCount.containsKey(owner)) {
            return tokenIssueCount.get(owner).incrementAndGet();
        } else {
            tokenIssueCount.putIfAbsent(owner, new AtomicInteger(1));
            return 1;
        }
    }

    private void addAccessToken(String owner, String token) {
        if (tokens.containsKey(owner)) {
            tokens.get(owner).add(token);
        } else {
            List<String> userTokens = new ArrayList<>();
            userTokens.add(token);
            tokens.put(owner, userTokens);
        }
    }

    boolean accessTokenExists(String owner, String token) {
        if (owner == null || !tokens.containsKey(owner)) {
            return false;
        }
        return tokens.get(owner).contains(token);
    }

    void clearAll() {
        clearClients();
        clearOwners();
        clearTokens();
        clearAccessCounters();
        clearTokenIssueCounters();
    }

    void clearClients() {
        clients.clear();
    }

    void clearOwners() {
        owners.clear();
    }

    void clearTokens() {
        tokens.clear();
    }

    void clearAccessCounters() {
        accessCount.clear();
    }

    void clearTokenIssueCounters() {
        tokenIssueCount.clear();
    }

    public void incrementResourceAccessCount(String owner) {
        if (accessCount.containsKey(owner)) {
            accessCount.get(owner).incrementAndGet();
        } else {
            accessCount.putIfAbsent(owner, new AtomicInteger(1));
        }
    }

    public int getResourceAccessCount(String owner) {
        return accessCount.getOrDefault(owner, new AtomicInteger(0)).get();
    }

    public int getTokenIssueCount(String owner) {
        return tokenIssueCount.getOrDefault(owner, new AtomicInteger(0)).get();
    }
}
