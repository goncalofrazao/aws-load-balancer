package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class LoadBalancer {
	public static float MAX_LOAD = 2.0f;
	private static List<Worker> workers = Collections.synchronizedList(new ArrayList<>());
	private static AtomicLong maxBasicBlocks = new AtomicLong(3);
	private static AtomicLong maxInstructionsCount = new AtomicLong(105);

	public static void printStats() {
		System.out.println("Max basic blocks: " + maxBasicBlocks.get());
		System.out.println("Max instructions count: " + maxInstructionsCount.get());
	}

	public static Worker chooseWorker(float complexity) {
		if (AutoScaler.SCALLING && complexity < 0.2f) {
			return null;
		}

		Worker chosenWorker = workers.get(0);
		for (Worker worker : workers) {
			if (worker.isActive() && worker.getLoad() < chosenWorker.getLoad()) {
				chosenWorker = worker;
			}
		}
		return chosenWorker;
	}
	
	public static float estimateComplexity(long imageSize, long oldImageSize, long basicBlocks, long instructionsCount) {
		// System.out.println("Estimating complexity: imageSize=" + imageSize + ", basicBlocks=" + basicBlocks + ", instructionsCount=" + instructionsCount);
		// printStats();
		maxBasicBlocks.getAndAccumulate(basicBlocks, Math::max);
		maxInstructionsCount.getAndAccumulate(instructionsCount, Math::max);

		return (float) imageSize / (float) oldImageSize * (((float) basicBlocks / (float) maxBasicBlocks.get()) + ((float) instructionsCount / (float) maxInstructionsCount.get()));
	}

	public static void addWorker(Worker worker) {
		workers.add(worker);
	}

	public static void removeWorker(Worker worker) {
		workers.remove(worker);
	}

	public static List<Worker> getWorkers() {
		return workers;
	}

	public static void listWorkers() {
		for (Worker worker : workers) {
			System.out.println(worker);
		}
	}
}
