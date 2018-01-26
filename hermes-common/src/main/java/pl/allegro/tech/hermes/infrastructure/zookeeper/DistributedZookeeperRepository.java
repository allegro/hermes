package pl.allegro.tech.hermes.infrastructure.zookeeper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor;

import java.io.IOException;
import java.util.List;

public abstract class DistributedZookeeperRepository {

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
}
