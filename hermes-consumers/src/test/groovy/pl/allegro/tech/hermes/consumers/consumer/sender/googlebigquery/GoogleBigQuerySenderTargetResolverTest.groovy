package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery

import com.google.cloud.bigquery.storage.v1.TableName
import pl.allegro.tech.hermes.api.EndpointAddress
import spock.lang.Specification

class GoogleBigQuerySenderTargetResolverTest  extends Specification {
    def 'should resolve endpoint address'() {

        given:
        EndpointAddress endpointAddress = EndpointAddress.of("googlebigquery://projects/project/datasets/dataset/tables/table")
        GoogleBigQuerySenderTargetResolver resolver = new GoogleBigQuerySenderTargetResolver()

        TableName expected = TableName.of("project", "dataset", "table")



        when:
        GoogleBigQuerySenderTarget target = resolver.resolve(endpointAddress)

        then:
        target.getTableName().project == expected.project
        target.getTableName().dataset == expected.dataset
        target.getTableName().table == expected.table
    }

    def 'should throw exception when endpoint address is invalid'() {

        given:
        EndpointAddress endpointAddress = EndpointAddress.of("googlebigquery://projects/project/datasets/dataset/tables/table/streams/_default")
        GoogleBigQuerySenderTargetResolver resolver = new GoogleBigQuerySenderTargetResolver()

        when:
        resolver.resolve(endpointAddress)

        then:
        thrown(IllegalArgumentException)

    }
}
