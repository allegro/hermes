package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json;

import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleBigQueryJsonWriteClientProvider {
    private final BigQueryWriteSettings writeSettings;
    public GoogleBigQueryJsonWriteClientProvider(BigQueryWriteSettings writeSettings) {
        this.writeSettings = writeSettings;
    }

    public BigQueryWriteClient getWriteClient() throws IOException {
        return BigQueryWriteClient.create(this.writeSettings);
    }
}
