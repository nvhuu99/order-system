# Setup:

    ✅ - setup monitoring stack
    ✅ - deploy cart service in k8s
    ✅ - deploy and run k6
    ✅ - create monitoring performance dashboard
        
# Performance testing:

### Refs:
* Tempo is consuming a lot of cpu and memory: https://github.com/grafana/tempo/discussions/1946

### Targets:

1. What are the bottlenecks?
  - CartService: high latency if **insufficient CPU**. With 1 core CPU, latency is very low for 100 requests/sec. 
  - Monitoring Stack: each service has their own infrastructure. But most of the collector (logs, traces, metrics) rely on storing telementries in **memory** before flushing to disk. Must control the flush rate properly (max block bytes, max idle time, ...) 
  - Redis: they have their own TCP Protocal, making them very efficient. The performance is very stable.
  - InventoryService: not enough data for now.

2. Services point of failure. Did the service restart, why?
  - CartService: their will not be a failure, but the latency depends on request rate and CPU amount. If we want to control the latency, we must employ **request throttle limiting** or **eventual consistent & event-driven**.

## Records:

### 10 requests/second: 

* ✅ passed, no failure

### 100 requests/second:

**Run#1:** 
  - Tempo distributor OOMKilled -> Increased memory.

**Run#2:**
  - Grafana OOMKilled: let's reduce the log entries limit, and keep the refresh rate at 30s.
  - Tempo Distributor OOMKilled: Could be network limit or disk i/o, leading to flushing rate can't keep up with incoming data. **Move to another task for tuning Tempo.**

### 35 requests/second:

**Run#3 + Run#4:**
  - Seeing the cart-service latency is ~1.5s/req, I increase the vCPU to 2 core (from 200-400ms).
  - Result: ✅ passed, no failure. Latency dropped to 25ms/req.

### 100 requests/second:

**Run#5**
  - Adjust cart-service vCPU to 1 core.
  - Result: ✅ app ok (although the latency is doubled ~50ms/req); ✅distributor restart once but OK, just increase memory for it.

