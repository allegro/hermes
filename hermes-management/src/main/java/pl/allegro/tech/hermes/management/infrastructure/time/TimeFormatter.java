package pl.allegro.tech.hermes.management.infrastructure.time;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class TimeFormatter {

    private static final Logger logger = LoggerFactory.getLogger(TimeFormatter.class);

    private final PeriodFormatter hourFormatter;
    private final Clock clock;

    @Autowired
    public TimeFormatter(Clock clock) {
        this.clock = clock;
        hourFormatter = new PeriodFormatterBuilder()
            .appendPrefix("-")
            .appendHours()
            .appendSuffix("h")
            .toFormatter();
    }

    public Long parse(String formattedTime) {
        try {
            Period period = hourFormatter.parsePeriod(formattedTime);
            return LocalDateTime.now().minusHours(period.getHours()).atZone(clock.getZone()).toInstant().toEpochMilli();
        } catch (IllegalArgumentException e) {
            logger.info("Could not parse period. {}", e.getMessage());
        }

        try {
            return LocalDateTime.parse(formattedTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(clock.getZone()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            logger.info("Could not parse date. {}", e.getMessage());
        }

        throw new IllegalArgumentException(
            "Could not parse given time. Available formats: period in hours (ex. \"-7h\") or ISO local date time"
        );
    }
}
