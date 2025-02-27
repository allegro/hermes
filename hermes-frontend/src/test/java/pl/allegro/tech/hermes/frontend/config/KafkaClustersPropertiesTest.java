package pl.allegro.tech.hermes.frontend.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.common.kafka.KafkaParameters;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;

public class KafkaClustersPropertiesTest {

  private KafkaClustersProperties kafkaClustersProperties;
  private DatacenterNameProvider datacenterNameProvider;

  @Before
  public void setUp() {
    kafkaClustersProperties = new KafkaClustersProperties();
    datacenterNameProvider = mock(DatacenterNameProvider.class);
  }

  @Test
  public void shouldReturnRemoteKafkaProperties() {
    // given
    KafkaProperties localCluster = new KafkaProperties();
    localCluster.setDatacenter("dc1");
    localCluster.setRemoteDatacenters(Arrays.asList("dc2", "dc3"));

    KafkaProperties remoteCluster1 = new KafkaProperties();
    remoteCluster1.setDatacenter("dc2");

    KafkaProperties remoteCluster2 = new KafkaProperties();
    remoteCluster2.setDatacenter("dc3");

    kafkaClustersProperties.setClusters(
        Arrays.asList(localCluster, remoteCluster1, remoteCluster2));
    when(datacenterNameProvider.getDatacenterName()).thenReturn("dc1");

    // when
    List<KafkaParameters> remoteKafkaProperties =
        kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);

    // then
    Set<String> remoteDCs =
        remoteKafkaProperties.stream()
            .map(KafkaParameters::getDatacenter)
            .collect(Collectors.toSet());
    assertEquals(2, remoteKafkaProperties.size());
    assertEquals(Set.of("dc2", "dc3"), remoteDCs);
  }

  @Test
  public void shouldThrowExceptionWhenNoPropertiesForDatacenter() {
    // given
    when(datacenterNameProvider.getDatacenterName()).thenReturn("dc1");

    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider));
  }

  @Test
  public void shouldReturnEmptyListWhenNoRemoteDatacenters() {
    // given
    KafkaProperties localCluster = new KafkaProperties();
    localCluster.setDatacenter("dc1");
    localCluster.setRemoteDatacenters(List.of());

    kafkaClustersProperties.setClusters(List.of(localCluster));
    when(datacenterNameProvider.getDatacenterName()).thenReturn("dc1");

    // when
    List<KafkaParameters> remoteKafkaProperties =
        kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);

    // then
    assertTrue(remoteKafkaProperties.isEmpty());
  }

  @Test
  public void shouldReturnOnlySpecifiedRemoteKafkaProperties() {
    // given
    KafkaProperties localCluster = new KafkaProperties();
    localCluster.setDatacenter("dc1");
    localCluster.setRemoteDatacenters(List.of("dc3"));

    KafkaProperties remoteCluster1 = new KafkaProperties();
    remoteCluster1.setDatacenter("dc2");

    KafkaProperties remoteCluster2 = new KafkaProperties();
    remoteCluster2.setDatacenter("dc3");

    kafkaClustersProperties.setClusters(
        Arrays.asList(localCluster, remoteCluster1, remoteCluster2));
    when(datacenterNameProvider.getDatacenterName()).thenReturn("dc1");

    // when
    List<KafkaParameters> remoteKafkaProperties =
        kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider);

    // then
    Set<String> remoteDCs =
        remoteKafkaProperties.stream()
            .map(KafkaParameters::getDatacenter)
            .collect(Collectors.toSet());
    assertEquals(1, remoteKafkaProperties.size());
    assertEquals(Set.of("dc3"), remoteDCs);
  }

  @Test
  public void shouldThrowExceptionForInvalidDatacenterName() {
    // given
    KafkaProperties localCluster = new KafkaProperties();
    localCluster.setDatacenter("dc1");
    localCluster.setRemoteDatacenters(Arrays.asList("dc2", "dc3"));

    kafkaClustersProperties.setClusters(List.of(localCluster));
    when(datacenterNameProvider.getDatacenterName()).thenReturn("invalid-dc");

    // when & then
    assertThrows(
        IllegalArgumentException.class,
        () -> kafkaClustersProperties.toRemoteKafkaProperties(datacenterNameProvider));
  }
}
