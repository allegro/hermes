package pl.allegro.tech.hermes.infrastructure.zookeeper.executor;

import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient;
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ExecutionResult.Status.FAILURE;
import static pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ExecutionResult.Status.SUCCESS;

public class ZookeeperCommandExecutor {
    private ZookeeperClientManager clientManager;
    private ExecutorService executor;
    private boolean rollbackEnabled;

    public ZookeeperCommandExecutor(ZookeeperClientManager clientManager,
                                    ExecutorService executor,
                                    boolean rollbackEnabled) {
        this.clientManager = clientManager;
        this.executor = executor;
        this.rollbackEnabled = rollbackEnabled;
    }

    public void execute(ZookeeperCommand command) {
        if (rollbackEnabled) {
            backup(command);
        }
        List<Future<ExecutionResult>> executions = submitExecutions(command);
        ExecutionResults results = waitForExecutionResults(executions);
        if (shouldRollback(results)) {
            List<ZookeeperClient> successfulClients = getClientsForExecutionsWithStatus(results, SUCCESS);
            rollback(successfulClients, command);
        }
        if (results.executionFailed()) {
            throw new ZookeeperCommandFailedException("Failed to execute command " +
                    command.getClass().getSimpleName(), results.getExceptions());
        }
    }

    private void backup(ZookeeperCommand command) {
        ZookeeperClient client = clientManager.getLocalClient();
        try {
            command.backup(client);
        } catch (Exception e) {
            throw new ZookeeperCommandFailedException("Backup procedure failed for command " +
                    command.getClass().getSimpleName() + " on client '" + client.getName() + "'.", e);
        }
    }

    private List<Future<ExecutionResult>> submitExecutions(ZookeeperCommand command) {
        return clientManager.getClients()
                .stream()
                .map(client -> executor.submit(new ExecutionCallable(client, command)))
                .collect(Collectors.toList());
    }

    private ExecutionResults waitForExecutionResults(List<Future<ExecutionResult>> executions) {
        return new ExecutionResults(executions
                .stream()
                .map(execution -> {
                    try {
                        return execution.get();
                    } catch (InterruptedException | ExecutionException e) {
                        return ExecutionResult.failure(e);
                    }
                })
                .collect(Collectors.toList())
        );
    }

    private List<ZookeeperClient> getClientsForExecutionsWithStatus(ExecutionResults results,
                                                                    ExecutionResult.Status status) {
        List<ZookeeperClient> clients = new ArrayList<>();
        for (int executionNumber = 0; executionNumber < results.getExecutionNumber(); executionNumber++) {
            ExecutionResult result = results.get(executionNumber);
            if (result.getStatus() == status) {
                clients.add(clientManager.getClients().get(executionNumber));
            }
        }
        return clients;
    }

    private boolean shouldRollback(ExecutionResults results) {
        return results.executionFailed() && rollbackEnabled;
    }

    private void rollback(List<ZookeeperClient> clients, ZookeeperCommand command) {
        List<Future<ExecutionResult>> rollbacks = clients
                .stream()
                .map(client -> executor.submit(new RollbackCallable(client, command)))
                .collect(Collectors.toList());
        ExecutionResults results = waitForExecutionResults(rollbacks);
        List<ZookeeperClient> failedClients = getClientsForExecutionsWithStatus(results, FAILURE);
        if (!failedClients.isEmpty()) {
            List<String> clientNames = failedClients
                    .stream()
                    .map(ZookeeperClient::getName)
                    .collect(Collectors.toList());
            List<Throwable> causes = results.getExceptions();
            throw new RollbackFailedException(clientNames, causes);
        }
    }

    private class ExecutionCallable implements Callable<ExecutionResult> {
        private ZookeeperClient client;
        private ZookeeperCommand command;

        ExecutionCallable(ZookeeperClient client, ZookeeperCommand command) {
            this.client = client;
            this.command = command;
        }

        @Override
        public ExecutionResult call() {
            try {
                command.execute(client);
                return ExecutionResult.success();
            } catch (Exception e) {
                return ExecutionResult.failure(e);
            }
        }
    }

    private class RollbackCallable implements Callable<ExecutionResult> {
        private ZookeeperClient client;
        private ZookeeperCommand command;

        RollbackCallable(ZookeeperClient client, ZookeeperCommand command) {
            this.client = client;
            this.command = command;
        }

        @Override
        public ExecutionResult call() {
            try {
                command.rollback(client);
                return ExecutionResult.success();
            } catch (Exception e) {
                return ExecutionResult.failure(e);
            }
        }
    }
}
