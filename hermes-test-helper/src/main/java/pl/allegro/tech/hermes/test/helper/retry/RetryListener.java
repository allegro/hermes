package pl.allegro.tech.hermes.test.helper.retry;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.TestListenerAdapter;

public class RetryListener extends TestListenerAdapter {

    @Override
    public void onTestFailure(ITestResult result) {
        IRetryAnalyzer analyzer = result.getMethod().getRetryAnalyzer(result);
        if (analyzer != null && analyzer instanceof Retry) {
            result.setStatus(((Retry) analyzer).isRetryAvailable() ? ITestResult.SKIP : ITestResult.FAILURE);
            Reporter.setCurrentTestResult(result);
        }
    }

}
