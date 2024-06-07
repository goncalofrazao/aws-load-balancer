package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Worker {
    private String id;
    private String dns;
    private AtomicBoolean active;
    private AtomicReference<Float> load;
    private AtomicInteger requests;
    private int healthCheck;

    public Worker(String id, String dns) {
        this.id = id;
        this.dns = dns;
        this.active = new AtomicBoolean(true);
        this.load = new AtomicReference<Float>(0.0f);
        this.requests = new AtomicInteger(0);
        this.healthCheck = 0;
    }

    public boolean isNotHealthy() {
        return this.healthCheck >= 3;
    }

    public void incrementHealthCheck() {
        this.healthCheck++;
    }

    public void resetHealthCheck() {
        this.healthCheck = 0;
    }

    public String getId() {
        return this.id;
    }

    public String getDNS() {
        return this.dns;
    }

    public void deactivate() {
        this.active.set(false);
    }

    public void activate() {
        this.active.set(true);
    }

    public boolean isActive() {
        return this.active.get();
    }

    public void addLoad(float load) {
        this.load.updateAndGet(currentLoad -> currentLoad + load);
        this.requests.incrementAndGet();
    }

    public void reduceLoad(float load) {
        this.load.updateAndGet(currentLoad -> currentLoad - load);
        this.requests.decrementAndGet();
    }

    public float getLoad() {
        return this.load.get();
    }

    public int getRequests() {
        return this.requests.get();
    }

    public String toString() {
        return "Worker " + this.id + " at " + this.dns + " with load " + this.load.get() + " and " + this.requests.get() + " requests";
    }
}
