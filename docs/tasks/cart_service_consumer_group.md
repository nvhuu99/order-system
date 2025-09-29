- move message pushing to shop app, init 3 consumer groups, push 10 requests, test request handle concurrently
  logs should include thread name
  observe the logs to see the work distribution

  + run multiple consumer on each instance: each join a different group
  + the group name should be fixed