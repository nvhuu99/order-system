### Error:
  
  > failed to load chunk 'ZmFrZS80MTBlYzE3NjFjOTFhOTdjOjE5OTcwNDhhMjlkOjE5OTcwNDhmZDkxOjUwZGRiN2Nl': open /var/loki/chunks/ZmFrZS80MTBlYzE3NjFjOTFhOTdjOjE5OTcwNDhhMjlkOjE5OTcwNDhmZDkxOjUwZGRiN2Nl: no such file or directory

### Reason:

  Refs: https://github.com/grafana/helm-charts/issues/1111

  > NOTE: In its default configuration, the chart uses boltdb-shipper and filesystem as storage. The reason for this is that the chart can be validated and installed in a CI pipeline. However, this setup is not fully functional. Querying will not be possible (or limited to the ingesters' in-memory caches) because that would otherwise require shared storage between ingesters and queriers which the chart does not support and would require a volume that supports ReadWriteMany access mode anyways. The recommendation is to use object storage, such as S3, GCS, MinIO, etc., or one of the other options documented at https://grafana.com/docs/loki/latest/storage/.

   > Solution:  Migrate from using local-storage, or create a mount directory on the host and share between components


