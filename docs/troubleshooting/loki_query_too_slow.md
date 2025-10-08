# Logs:
  Current stack is grafana/loki-distributed -> default storage is localfile -> no index -> very slow

  MinIO implements the same API as Amazon S3, so software that can talk to S3 can also talk to MinIO without changing the code.
    We can use it for Loki backend.
  
  I tried to migrate to object-storage using Minio, but the configurations is very hard.

  It's seem easier to use grafana/loki instead grafana/loki-distributed, especially with mode=SingleBinary.

  mode=SingleBinary is not so "single" at all, it still creates separated components, with limited configuration.

  Back with grafana/loki-distributed.
  
# Loki distributions:

    Communiti Ver: https://github.com/grafana/helm-charts/tree/main/charts/loki-stack
        * Come with Promtail, Grafana, ...

    Grafana Loki: https://github.com/grafana/loki/blob/main/production/helm/loki/values.yaml
    
    Grafana Loki Distributed: https://github.com/grafana/helm-charts/blob/main/charts/loki-distributed/values.yaml
        + better avaiability: sharding, replication, each component can have multiple instances

# Loki's architecture & read path:

    https://grafana.com/docs/loki/latest/get-started/architecture/#read-path

    1. The querier will fetch all chunks from the ingester's "in-memory-chunks"
    2. If not enough data in mem, it fetch lazily from back-end storage
    => Default setup use filesystem as backend -> the ingester is the only

# Conclusion:

    For production, use Object Storage  (shared between all components)
    For test/dev, use filesystem, but provide the ingester sufficient memory to avoid IO
        this way, the query speed will be improve.
    Also, kept the query range minimal (30mins). Ofcourse it depend on the amount of logs
    being pushed.