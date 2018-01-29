package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.HermesException;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandFailedException;

import java.io.IOException;
import java.util.List;

public abstract class DistributedZookeeperRepository {
    private static final Logger logger = LoggerFactory.getLogger(DistributedZookeeperRepository.class);

    protected ZookeeperClientManager clientManager;
    protected ObjectMapper mapper;

    protected DistributedZookeeperRepository(ZookeeperClientManager clientManager, ObjectMapper mapper) {
        this.clientManager = clientManager;
        this.mapper = mapper;
    }

    <T> T unmarshall(byte[] data, Class<T> clazz) {
        try {
            return mapper.readValue(data, clazz);
        } catch (IOException e) {
            throw new InternalProcessingException("Could not unmarshall Group.", e);
        }
    }

    <T> T unmarshall(byte[] data, TypeReference<T> type) {
        try {
            return mapper.readValue(data, type);
        } catch (IOException e) {
            throw new InternalProcessingException("Could not parse json.", e);
        }
    }

    <T> byte[] marshall(T object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new InternalProcessingException("Could not marshall " + object.getClass().getSimpleName() + ".", e);
        }
    }

    protected void executeWithErrorHandling(ZookeeperCommandExecutor executor, ZookeeperCommand command) {
        try {
            executor.execute(command);
        } catch (ZookeeperCommandFailedException e) {
            List<Throwable> causes = e.getCauses();
            logCauses(causes, command.getClass().getSimpleName());
            throw wrapInInternalProcessingExceptionIfNeeded(causes.get(0));
        }
    }

    private RuntimeException wrapInInternalProcessingExceptionIfNeeded(Throwable cause) {
        return cause instanceof HermesException ? (HermesException)cause : new InternalProcessingException(cause);
    }

    private void logCauses(List<Throwable> causes, String commandName) {
        causes.forEach(cause -> logger.warn("Failed to execute command {}.", commandName, cause));
    }
}
