package pl.allegro.tech.hermes.infrastructure.zookeeper.executor;

import java.util.List;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ExecutionResult.Status.FAILURE;

public class ExecutionResults {

    private final List<ExecutionResult> results;

    public ExecutionResults(List<ExecutionResult> results) {
        this.results = results;
    }

    public boolean executionFailed() {
        return results.stream().anyMatch(result -> result.getStatus() == FAILURE);
    }

    public List<Throwable> getExceptions() {
        return results.stream()
                .filter(result -> result.getStatus() == FAILURE)
                .map(ExecutionResult::getException)
                .collect(Collectors.toList());
    }

    public ExecutionResult get(int index) {
        return results.get(index);
    }

    public int getExecutionNumber() {
        return results.size();
    }

}
