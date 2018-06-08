package pl.allegro.tech.hermes.consumers.supervisor.process;

import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import pl.allegro.tech.hermes.api.SubscriptionName;

import static java.util.stream.Collectors.toList;

class ManagedConsumerProcesses {

	private final Map<SubscriptionName, RunningProcess> runningProcesses = new ConcurrentHashMap<>();

	private final Map<SubscriptionName, DyingProcess> dyingProcesses = new ConcurrentHashMap<>();

	private final Clock clock;

	ManagedConsumerProcesses(Clock clock) {
		this.clock = clock;
	}

	List<ManagedSubscriptionStatus> listRunningSubscriptionsStatus() {
		return listSubscriptionsStatus(runningProcesses);
	}

	int countRunningSubscriptions() {
		return runningProcesses.size();
	}

	Set<SubscriptionName> runningSubscriptions() {
		return ImmutableSet.copyOf(runningProcesses.keySet());
	}

	Set<SubscriptionName> dyingSubscriptions() {
		return ImmutableSet.copyOf(dyingProcesses.keySet());
	}

	Stream<ConsumerProcess> runningProcessesStream() {
		return runningProcesses.values().stream()
				.map(RunningProcess::getProcess);
	}

	Stream<ConsumerProcess> dyingProcessesStream() {
		return dyingProcesses.values().stream()
				.map(DyingProcess::getProcess);
	}

	Stream<ConsumerProcess> unhealthyProcessesStream() {
		return runningProcessesStream().filter(consumerProcess -> !consumerProcess.isHealthy());
	}

	Stream<ConsumerProcess> toForceKillProcessesStream() {
		return dyingProcesses.values().stream()
				.filter(dyingProcess -> dyingProcess.getKillTime() < clock.millis())
				.map(DyingProcess::getProcess);
	}

	long killTimeForDyingProcess(ConsumerProcess process) {
		return dyingProcesses.get(process.getSubscription().getQualifiedName()).killTime;
	}

	boolean hasRunningProcess(Signal signal) {
		return runningProcesses.containsKey(signal.getTarget());
	}

	boolean hasDyingProcess(Signal signal) {
		return dyingProcesses.containsKey(signal.getTarget());
	}

	ConsumerProcess getRunningProcess(Signal signal) {
		return runningProcesses.get(subscriptionName(signal)).getProcess();
	}

	ConsumerProcess getDyingProcess(Signal signal) {
		return dyingProcesses.get(subscriptionName(signal)).getProcess();
	}

	void add(ConsumerProcess process, Future executionHandle) {
		this.runningProcesses.put(
				process.getSubscription().getQualifiedName(), new RunningProcess(process, executionHandle));
	}

	void moveToDyingProcesses(Signal signal, long shouldBeKilledAfter) {
		dyingProcesses.put(
				subscriptionName(signal),
				DyingProcess.from(runningProcesses.remove(subscriptionName(signal)), shouldBeKilledAfter));
	}

	Future getRunningExecutionHandle(Signal signal) {
		return runningProcesses.get(subscriptionName(signal)).getExecutionHandle();
	}

	Future getDyingExecutionHandle(Signal signal) {
		return dyingProcesses.get(subscriptionName(signal)).getExecutionHandle();
	}

	boolean processExists(Signal signal) {
		return hasDyingProcess(signal) || hasRunningProcess(signal);
	}

	void clean(Signal signal) {
		dyingProcesses.remove(subscriptionName(signal));
	}

	private static List<ManagedSubscriptionStatus> listSubscriptionsStatus(
			Map<SubscriptionName, ? extends RunningProcess> processes) {
		return processes.entrySet().stream()
				.map(entry -> new ManagedSubscriptionStatus(
						entry.getKey().getQualifiedName(),
						entry.getValue().getProcess().getSignalTimesheet()))
				.sorted((s1, s2) -> String.CASE_INSENSITIVE_ORDER.compare(s1.getQualifiedName(), s2.getQualifiedName()))
				.collect(toList());
	}

	private SubscriptionName subscriptionName(Signal signal) {
		return signal.getTarget();
	}

	private static class RunningProcess {
		private final ConsumerProcess process;

		private final Future executionHandle;

		RunningProcess(ConsumerProcess process, Future executionHandle) {
			this.process = process;
			this.executionHandle = executionHandle;
		}

		ConsumerProcess getProcess() {
			return process;
		}

		Future getExecutionHandle() {
			return executionHandle;
		}
	}

	private static class DyingProcess extends RunningProcess {

		private final long killTime;

		DyingProcess(ConsumerProcess process, Future executionHandle, long killTime) {
			super(process, executionHandle);
			this.killTime = killTime;
		}

		long getKillTime() {
			return killTime;
		}

		static DyingProcess from(RunningProcess runningProcess, long killTime) {
			return new DyingProcess(runningProcess.getProcess(), runningProcess.getExecutionHandle(), killTime);
		}
	}
}
