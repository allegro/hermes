package pl.allegro.tech.hermes.test.helper.retry;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import static java.lang.Integer.parseInt;

public class Retry implements IRetryAnalyzer {
    private int retryCount = 0;
    private static int maxRetryCount = parseInt(System.getProperty("tests.retry.count", "0"));

    @Override
    public boolean retry(ITestResult result) {
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
