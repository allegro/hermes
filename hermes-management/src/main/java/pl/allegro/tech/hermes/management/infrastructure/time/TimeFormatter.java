package pl.allegro.tech.hermes.management.infrastructure.time;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class TimeFormatter {

    private static final Logger logger = LoggerFactory.getLogger(TimeFormatter.class);

    private final String dateTimeFormatPattern = "yyyy-MM-dd HH:mm:ss";

    private final PeriodFormatter hourFormatter;
    private final DateFormat dateFormat;

    public TimeFormatter() {
        hourFormatter = new PeriodFormatterBuilder()
            .appendPrefix("-")
            .appendHours()
            .appendSuffix("h")
            .toFormatter();

        dateFormat = new SimpleDateFormat(dateTimeFormatPattern);
    }

    public Long parse(String formattedTime) {
        try {
            Period period = hourFormatter.parsePeriod(formattedTime);
            return LocalDateTime.now().minusHours(period.getHours()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (IllegalArgumentException e) {
            logger.info("Could not parse period. {}", e.getMessage());
        }

        try {
            return dateFormat.parse(formattedTime).toInstant().toEpochMilli();
        } catch (ParseException e) {
            logger.info("Could not parse date. {}", e.getMessage());
        }

        throw new IllegalArgumentException(
            "Could not parse given time. Available formats: period in hours (ex. \"-7h\") or date time with pattern " + dateTimeFormatPattern
        );
    }
}
