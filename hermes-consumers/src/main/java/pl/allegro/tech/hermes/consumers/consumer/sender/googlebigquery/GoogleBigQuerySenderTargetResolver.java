package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.cloud.bigquery.storage.v1.TableName;
import com.google.common.base.Preconditions;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.sender.SenderTarget;

import java.net.URI;

public class GoogleBigQuerySenderTargetResolver {

    public static final String GOOGLE_BQ_PROTOCOL = "googlebigquery";

    private static final String HOST = "projects";

    GoogleBigQuerySenderTarget resolve(EndpointAddress endpointAddress) {
        try {
            final URI endpointUri = URI.create(endpointAddress.getRawEndpoint());

            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("scheme: " + endpointUri.getScheme());
            System.out.println("host: " + endpointUri.getHost());
            System.out.println("port: " + endpointUri.getPort());
            System.out.println("path: " + endpointUri.getPath());
            System.out.println();
            System.out.println();
            System.out.println();

            Preconditions.checkArgument(endpointUri.getScheme().equals(GOOGLE_BQ_PROTOCOL));
            Preconditions.checkArgument(endpointUri.getHost().equals(HOST));
            Preconditions.checkArgument(endpointUri.getPort() <= 0);
            final String tableNamePath = endpointUri.getHost() + endpointUri.getPath();

            final TableName tableName = TableName.parse(tableNamePath);

            return GoogleBigQuerySenderTarget.newBuilder()
                    .withTableName(tableName)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Given endpoint is invalid", e);
        }
    }
}
