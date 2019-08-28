package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ZookeeperWorkloadConstraintsPathChildrenCacheTest extends ZookeeperBaseTest {

    private ZookeeperWorkloadConstraintsPathChildrenCache pathChildrenCache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        try {
            deleteAllNodes();
        } catch (Exception e) {
            e.printStackTrace();
        }

        pathChildrenCache = new ZookeeperWorkloadConstraintsPathChildrenCache(zookeeperClient, "/hermes/consumers-workload-constraints");
        pathChildrenCache.start();
    }

    @After
    public void cleanup() throws Exception {
        pathChildrenCache.close();
    }

    @Test
    public void shouldReturnEmptyListIfBasePathDoesNotExistTest() {
        assertThat(pathChildrenCache.getChildrenData()).isEmpty();
    }

    @Test
    public void shouldReturnListOfChildDataTest() throws Exception {
        // given
        setupNode("/hermes/consumers-workload-constraints/group.topic", new Constraints(1));
        setupNode("/hermes/consumers-workload-constraints/group.topic$sub", new Constraints(3));
        ensureCacheWasUpdated(2);

        // when
        Collection<ChildData> childrenData = pathChildrenCache.getChildrenData();
        List<Constraints> constraints = childrenData.stream()
                .map(ChildData::getData)
                .map(bytesToConstraints)
                .collect(toList());

        // then
        assertThat(childrenData).hasSize(2);
        assertThat(constraints).containsOnly(new Constraints(1), new Constraints(3));
    }

    @Test
    public void shouldUpdateCacheOnCreateNodeTest() throws Exception {
        // when
        setupNode("/hermes/consumers-workload-constraints/group.topic", new Constraints(1));
        ensureCacheWasUpdated(1);
        Collection<ChildData> childrenData = pathChildrenCache.getChildrenData();

        // then
        assertThat(childrenData).hasSize(1);

        // when
        setupNode("/hermes/consumers-workload-constraints/group.topic$sub", new Constraints(3));
        ensureCacheWasUpdated(2);
        Collection<ChildData> updatedChildrenData = pathChildrenCache.getChildrenData();

        // then
        assertThat(updatedChildrenData).hasSize(2);
    }

    @Test
    public void shouldUpdateCacheOnDeleteNodeTest() throws Exception {
        // when
        setupNode("/hermes/consumers-workload-constraints/group.topic", new Constraints(1));
        setupNode("/hermes/consumers-workload-constraints/group.topic$sub", new Constraints(3));
        ensureCacheWasUpdated(2);
        Collection<ChildData> childrenData = pathChildrenCache.getChildrenData();

        // then
        assertThat(childrenData).hasSize(2);

        // when
        deleteData("/hermes/consumers-workload-constraints/group.topic");
        ensureCacheWasUpdated(1);
        Collection<ChildData> updatedChildrenData = pathChildrenCache.getChildrenData();

        // then
        assertThat(updatedChildrenData).hasSize(1);
    }

    @Test
    public void shouldUpdateCacheOnChangeNodeTest() throws Exception {
        // given
        setupNode("/hermes/consumers-workload-constraints/group.topic", new Constraints(1));
        setupNode("/hermes/consumers-workload-constraints/group.topic$sub", new Constraints(3));
        ensureCacheWasUpdated(2);

        // when
        Collection<ChildData> childrenData = pathChildrenCache.getChildrenData();
        List<Constraints> constraints = childrenData.stream()
                .map(ChildData::getData)
                .map(bytesToConstraints)
                .collect(toList());

        // then
        assertThat(childrenData).hasSize(2);
        assertThat(constraints).containsOnly(new Constraints(1), new Constraints(3));

        // when
        updateNode("/hermes/consumers-workload-constraints/group.topic", new Constraints(2));
        ensureCacheWasUpdated(2);

        Collection<ChildData> updatedChildrenData = pathChildrenCache.getChildrenData();
        List<Constraints> updatedConstraints = updatedChildrenData.stream()
                .map(ChildData::getData)
                .map(bytesToConstraints)
                .collect(toList());

        // then
        assertThat(updatedChildrenData).hasSize(2);
        assertThat(updatedConstraints).containsOnly(new Constraints(2), new Constraints(3));
    }

    private void ensureCacheWasUpdated(int expectedSize) {
        await()
                .atMost(200, TimeUnit.MILLISECONDS)
                .until(() -> pathChildrenCache.getChildrenData().size() == expectedSize);
    }

    private final Function<byte[], Constraints> bytesToConstraints = bytes -> {
        try {
            return objectMapper.readValue(bytes, Constraints.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
}
