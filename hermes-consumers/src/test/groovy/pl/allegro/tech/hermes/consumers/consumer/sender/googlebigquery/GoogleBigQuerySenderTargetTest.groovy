package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery

import com.google.cloud.bigquery.storage.v1.TableName
import spock.lang.Specification

class GoogleBigQuerySenderTargetTest extends Specification{
    def 'should create sender target'(){

        given:
        TableName tableName = TableName.of("project", "dataset", "table")

        when:
        GoogleBigQuerySenderTarget target = GoogleBigQuerySenderTarget.newBuilder().withTableName(tableName).build()

        then:
        tableName == target.tableName
    }
}
