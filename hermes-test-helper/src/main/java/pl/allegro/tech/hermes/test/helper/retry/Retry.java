package pl.allegro.tech.hermes.test.helper.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import static java.lang.Integer.parseInt;

public class Retry implements IRetryAnalyzer {
    private int retryCount = 0;
    private static final int maxRetryCount = parseInt(System.getProperty("tests.retry.count", "2"));
    private static final Logger logger = LoggerFactory.getLogger(Retry.class);

    @Override
    public boolean retry(ITestResult result) {
        logger.error("Retrying test {}.{}", result.getTestClass().getName(), result.getMethod().getMethodName(), result.getThrowable());
        if (isRetryAvailable()) {
            retryCount++;
            return true;
        }
        return false;
    }

    public boolean isRetryAvailable() {
        return retryCount < maxRetryCount;
    }
}
