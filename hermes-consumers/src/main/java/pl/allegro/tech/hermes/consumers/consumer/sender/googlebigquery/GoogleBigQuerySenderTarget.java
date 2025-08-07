package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.cloud.bigquery.storage.v1.TableName;
import pl.allegro.tech.hermes.consumers.consumer.sender.SenderTarget;

import java.util.Objects;

public class GoogleBigQuerySenderTarget implements SenderTarget {

    private final TableName tableName;

    private GoogleBigQuerySenderTarget(TableName tableName) {
        this.tableName = tableName;
    }

    public TableName getTableName() {
        return tableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoogleBigQuerySenderTarget that = (GoogleBigQuerySenderTarget) o;
        return Objects.equals(tableName, that.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private TableName tableName;

        public Builder withTableName(TableName tableName) {
            this.tableName = tableName;
            return this;
        }

        public GoogleBigQuerySenderTarget build() {
            return new GoogleBigQuerySenderTarget(tableName);
        }
    }
}
