Tempo Distributor OOMKilled: Could be network limit or disk i/o, leading to flushing rate can't keep up with incoming data.

### Refs:

- Tempo is consuming a lot of cpu and memory: https://github.com/grafana/tempo/discussions/1946
- Tempo arch: https://grafana.com/docs/tempo/latest/operations/architecture/

### Understand the flow:

**Write path:**
  - Distributor: depend on how fast MetricsGenerators and Ingesters can accept spans.
  - Ingester: if flush more frequently, memory usage will be reduced, but more pressure on Compactor.
  - MetricsGenerator: may depend on how fast RemoteWrite response.

### Current workload:

**With 6 spans/req:**
  - Observed rate: 35 req/s => 210 spans/s
  - Expected rate: 100 req/s => 600 spans/s

**Let's reproduce:**
  - Rate: 35 reqs/s => 210 spans/s
  - Capture Usage: CPU, Memory, Disk I/O, Network

**Tuning #1:**
  - Ingester:
      - CPU: 25 - 50ms
      - Memory: 128 - 256MB
          - **trace_idle_period**: reduce from 30s to 10s, amount of span kept in memory will be 200 spans (before 600 spans).
          - **flush_check_period**: reduce from 30s to 10s.
  - **Result:**
      - Metrics: (see video)
      - Memory: All components survived the test. But strangely, Ingester got OOMKilled later. I must replay the test, this time,

    - **Follow the logs to see which operation lead to OOMKilled:**.
    
        - Conclusion: 
            - OOMKilled when running the job + distributor flushing
            - To avoid, just restart Tempo before running load test.

        ```
        Observations:
        - 3:25: distributor_mem_32, ingester_mem_32, metric_generator_32
        - 3:30: start job
        - 3:40: distributor_mem_128, ingester_mem_100, metric_generator_64
        - 3:40: end job
        - 3:58: ingester_mem_384, distributor_mem_128, metric_generator_64
    
        Distributor log events:
        - 3:20: started
        - the rest of logs: initiating cleanup of obsolete entries
    
        Ingester log events:
        - 3:20: started
        - 3:58: "head block cut. enqueueing flush op"..."flushing block"
          => after 30 minutes from started, the ingester start flushing
          => OOMKilled when running the job + distributor flushing
        ```


    