- move message pushing to shop app, init 3 consumer groups, push 10 requests, test request handle concurrently
  logs should include thread name
  observe the logs to see the work distribution

  + run multiple consumer on each instance: each join a different group
  + the group name should be fixed

===
# 2 replicas - 10 VUs - 100 reqs/vu:

  ### RESULT:
    
    rate: ~100req/second
    latency: 10-20 ms/req

  ### RUN #1:

    cart service race condition
    
      c0 -> acquire lock -> hanling
      c1 -> fail acquire lock -> skip
      ...
      c1 -> acquire lock -> ver mismatch
      c0 -> fail acquire lock -> skip
      ..
      => c0, c1 now can not proceed any request without invalid version exception
      
      solution-1: once a consumer skip a cart, do not try to acquire lock of that cart in the after
        + must test carefully, ensure equally balance the request between the consumers
  
      solution-2: one-to-one relationship between consumer and partition ⛔
        + if its take longer to handle a request, the next cart will have to wait
    
  ### RUN#2:

    unbalance:
      cart-service-0: handled all 1000 requests
      cart-service-1: skipped all 1000 requests
      => due to lock release fail error

    lock release failed:
      04:11:37.602 - ERROR - c.e.c.s.c.CartUpdateRequestHandler - user_id=VU_1_cart-update-load-test-95m75 - cart_ver=16 - handler=consumer-cart-service-1-1 - Lock release failed: Did not observe any item or terminal signal within 10000ms in 'retryWhen' (and no fallback has been configured)

    release failed but still commit:
      04:27:37.198 - INFO  - c.e.c.s.c.CartEventsConsumer - key=VU_9_cart-update-load-test-95m75 - partition=0 - group=cart-service-1 - offset=5504 - hostname=cart-service-1 - Message received
      04:27:37.199 - INFO  - c.e.c.s.c.CartUpdateRequestHandler - user_id=VU_9_cart-update-load-test-95m75 - cart_ver=1 - handler=consumer-cart-service-1-0 - Handling cart update request
      04:27:38.628 - ERROR - c.e.c.s.c.CartUpdateRequestHandler - user_id=VU_9_cart-update-load-test-95m75 - cart_ver=1 - handler=consumer-cart-service-1-0 - Build cart failed: Cart update request version (1) must be one unit ahead the current version (100)
      04:27:48.636 - ERROR - c.e.c.s.c.CartUpdateRequestHandler - user_id=VU_9_cart-update-load-test-95m75 - cart_ver=1 - handler=consumer-cart-service-1-0 - Lock release failed: Did not observe any item or terminal signal within 10000ms in 'retryWhen' (and no fallback has been configured)
      04:27:48.637 - INFO  - c.e.c.s.c.CartEventsConsumer - Message committed

  ### RUN#3:
    
      all instances work correctly

      low latency:
        - request total time: ~9s
        - workload finished latency: ~2s

      low to medium unbalance:
        - 500 - 500
        - 500 - 500
        - 600 - 400
        - 900 - 100

# 4 replicas - 10 VUs - 100 reqs/vu:

  ### RUN#1:
      
      failed: race condition again, cart got skip
      solution: once you lock a user cart, never release the lock

  ### RUN#2:
    
      all cart request success ✅
      performance very good as there are less steps now, and more replicas:
        - request total time: ~26s
        - workload finished latency: ~27s
        - latency avg: 3-5ms
      request balance (undetermined):
        - 500, 100, 200, 200
        - 300, 200, 200, 100
        - 600, 400, 0, 0
        - 100, 600, 200, 100