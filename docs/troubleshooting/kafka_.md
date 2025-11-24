        Current setting: 1. auto-commit: false 2. ack-mode: manual_immediate 3. max-poll-records: 1 I am expecting the following behaviors: - only one message per poll - the listener function will trigger a reactor Mono to handle the message and return immediately, but it will not ack the message. The Mono must decide to ack or not. - if the Mono did not ack, it want to re-process the message when the listener got the message re-deliver by the poll() But currently: - if the Mono did not ack, the poll() give the next message instead of redeliver



You're very close — the behavior you're seeing matches how the Kafka consumer and the Spring Kafka container actually work. I'll explain *why* the next message is delivered even though the Mono didn't `ack()`, and give three practical ways to get the behaviour you want (redeliver the same record until the Mono acks it).

## Why you get the next message even if you don't `ack()`

1. **`poll()` advances the consumer position in-memory when records are returned.** Kafka’s `poll()` hands you records and the consumer's *position* moves forward; that is independent from whether you *commit* the offset back to the broker. In other words, the consumer already "consumed" that offset locally and will proceed to the next offset on subsequent polls. ([Apache Kafka][1])

2. **`ACK` (commit) vs consumer position:** Not calling `Acknowledgment.acknowledge()` just means the broker doesn't get a committed offset. Redelivery typically happens only when the consumer group restarts / rebalances or you `seek()` back to an earlier offset (or you stop the consumer and restart it). It is not triggered automatically just because you didn't call `ack()` for a polled record. ([GitHub][2])

3. **Spring Kafka container loop:** The listener container drives `poll()` in a loop. If your `@KafkaListener` returns quickly (because it launches a Reactor `Mono` and returns), the container loop continues and polls again — returning the next record (even with `max.poll.records=1`). The container does *not* wait for your asynchronous Mono to ack or finish before calling `poll()` again unless you explicitly prevent it (pause/block). (Spring docs and the project discussions make this explicit for manual ack usage and async processing.) ([Home][3])

## So — how to get “redeliver until ack” behavior

Below are practical options (ordered by how safe/practical they are).

### Option A — **Pause the container / partitions while the Mono runs, resume when done**

This is the cleanest approach that preserves non-blocking execution of your reactive pipeline while preventing the consumer from polling further messages.

* Use `KafkaListenerEndpointRegistry` to get the container by `id` (set `id` on `@KafkaListener`) and call `pause()` before starting the async work and `resume()` when the Mono completes (ack or not). Per Spring docs, `pause()` takes effect just before the next `poll()` and `resume()` takes effect just after current `poll()` returns. That prevents fetching the next message while your Mono is deciding. ([Home][4])

Example sketch:

```java
@Autowired
private KafkaListenerEndpointRegistry registry;

@KafkaListener(id = "myListener", topics = "topic", containerFactory = "kafkaListenerContainerFactory")
public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
    var container = registry.getListenerContainer("myListener");
    container.pause(); // prevents further fetches

    processMono(record) // returns Mono<Void>
      .doOnSuccess(v -> ack.acknowledge())
      .doFinally(sig -> container.resume()) // always resume
      .subscribe(); // keep reactive non-blocking
}
```

**Notes & caveats**

* `pause()`/`resume()` are safe (avoid rebalances) and are intended for this use. ([Home][4])
* If you pause and never resume (or resume late) you risk stalling message consumption and increasing consumer group lag.
* This works well when you want exactly one outstanding message per consumer.

### Option B — **Process synchronously on the consumer thread (block until Mono completes)**

Simpler but blocks the consumer thread — not recommended if processing is long or you need high throughput.

```java
@KafkaListener(...)
public void listen(ConsumerRecord<String,String> record, Acknowledgment ack) {
    try {
        processMono(record).block(); // blocking — consumer thread
        ack.acknowledge();
    } catch (Exception e) {
        // decide no ack -> on restart/redelivery
    }
}
```

**When to use:** short, fast processing only. Blocking risks hitting `max.poll.interval.ms` if you block too long and may trigger rebalances.

### Option C — **Use Reactor Kafka (reactive Kafka client) which supports reactive back-pressure**

If you want full reactive control (no manual pause/resume hacks), consider using Reactor Kafka. It ties the Kafka consumer lifecycle into Reactor back-pressure so you can avoid polling new records until your pipeline signals demand. This is a more involved change but a good fit for reactive apps. ([projectreactor.io][5])

### Option D — (advanced) Seek back when you want re-delivery

You can call the Kafka `Consumer` API (`seek(topicPartition, offset)`) to move the consumer position back to the un-acked offset when your Mono fails/declines to ack. This is error-prone (careful with races & partition bookkeeping) and usually not necessary if you can use pause/resume or Reactor Kafka.

## Extra notes about config you already have

* `max.poll.records = 1` ensures each poll returns at most one record — but it does **not** prevent the container from polling again and returning the next offset. The in-memory position still moves forward on poll. ([Apache Kafka][1])
* `auto.commit = false` simply disables auto commits; it doesn't stop the consumer position from advancing on poll. The broker-stored committed offset remains unchanged until you explicitly `acknowledge()` (or do a commit), meaning a restart/rebalance will redeliver uncommitted messages. ([GitHub][2])
* `AckMode.MANUAL_IMMEDIATE` will commit immediately if `acknowledge()` is called on the consumer thread — but it doesn't control whether the container will poll again while async work is in progress. ([Home][6])

## Recommendation (practical)

If you want exactly this behaviour — "listener returns immediately, Mono later decides to ack, and while Mono is deciding the consumer must not fetch the next message" — use **Option A: pause the container (or the topic-partition)** before launching the Mono and resume when the Mono completes. It keeps processing non-blocking and ensures no further polls deliver new messages until you explicitly allow them.

If you'd rather not touch pause/resume logic, and your processing is short, use **Option B** (block) as the quickest fix — but be aware of `max.poll.interval.ms` and throughput impacts.

If you want a long-term reactive-friendly solution, evaluate **Reactor Kafka** for native reactive back-pressure and better semantics for this use-case.

---

If you want, I can:

* give a copy-pasteable example with how to wire `KafkaListenerEndpointRegistry` and the @KafkaListener `id`, or
* sketch how a safe pause/resume with retry/timeouts would look in your current code.

Which example would you like me to produce now?

[1]: https://kafka.apache.org/25/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html?utm_source=chatgpt.com "KafkaConsumer (kafka 2.5.0 API)"
[2]: https://github.com/spring-projects/spring-kafka/discussions/2519?utm_source=chatgpt.com "In AckMode MANUAL_IMMEDIATE, is acknowledgment. ..."
[3]: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/ooo-commits.html?utm_source=chatgpt.com "Manually Committing Offsets :: Spring Kafka"
[4]: https://docs.spring.io/spring-kafka/reference/kafka/pause-resume.html?utm_source=chatgpt.com "Pausing and Resuming Listener Containers :: Spring Kafka"
[5]: https://projectreactor.io/docs/kafka/release/reference/?utm_source=chatgpt.com "Reactor Kafka Reference Guide"
[6]: https://docs.spring.io/spring-kafka/api/org/springframework/kafka/listener/ContainerProperties.AckMode.html?utm_source=chatgpt.com "Enum Class ContainerProperties.AckMode - Spring"



===


Kafka Experiment
	+ Reproduce the problem
	+ Solution 1: Block the consumer thread, expect the operations still execute in different threads
	+ Solution 2: Pause the container, if fails, seek() to reset
	+ How long until a rebalance if not commit, or only when restart, rebalance happens
	? why the fuck only-one message is deliver, but the offset is advance when not commit
        => No its totally not, it can deliver two messages at a time even when poll-max=1, here an example
                57.784 - INFO  - Reservation - key=VU_3_1763785846339_15074 - partition=0 - group=inventory-service - offset=40647 - hostname=inventory-service - Message received
                2025-11-22 11:30:57.784 | 04:30:57.784 - INFO  - Reservation - product_id=f7b2bf24-900d-44a7-a29c-f8bec6a68b53 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - handling reservation request
                2025-11-22 11:30:57.793 | 04:30:57.793 - DEBUG - Reservation - product_id=f7b2bf24-900d-44a7-a29c-f8bec6a68b53 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - get reservation successfully
                2025-11-22 11:30:57.793 | 04:30:57.793 - DEBUG - Reservation - product_id=f7b2bf24-900d-44a7-a29c-f8bec6a68b53 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - request validation success
                2025-11-22 11:30:57.795 | 04:30:57.795 - ERROR - Reservation - product_id=f7b2bf24-900d-44a7-a29c-f8bec6a68b53 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - try acquire handler lock failed - lock unavailable - collection: order-system:locks:order_system:reservation_request_handlers
                2025-11-22 11:30:57.796 | 04:30:57.796 - DEBUG - Reservation - product_id=f7b2bf24-900d-44a7-a29c-f8bec6a68b53 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - lock release success - order_system:product_availabilities
                2025-11-22 11:30:57.844 | 04:30:57.843 - DEBUG - Reservation - product_id=f7b2bf24-900d-44a7-a29c-f8bec6a68b53 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - lock release success - order_system:product_reservations
                2025-11-22 11:30:57.845 | 04:30:57.844 - INFO  - Reservation - key=VU_3_1763785846339_15074 - partition=0 - group=inventory-service - offset=40648 - hostname=inventory-service - Message received
                2025-11-22 11:30:57.845 | 04:30:57.845 - INFO  - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - handling reservation request
                2025-11-22 11:30:57.845 | 04:30:57.845 - DEBUG - Reservation - product_id=f7b2bf24-900d-44a7-a29c-f8bec6a68b53 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - lock release success - order_system:reservation_request_handlers
                2025-11-22 11:30:57.845 | 04:30:57.845 - INFO  - Reservation - key=VU_3_1763785846339_15074 - partition=0 - group=inventory-service - offset=40647 - hostname=inventory-service - Message committed
                2025-11-22 11:30:57.851 | 04:30:57.850 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - get reservation successfully
                2025-11-22 11:30:57.851 | 04:30:57.850 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - request validation success
                2025-11-22 11:30:57.851 | 04:30:57.851 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - try acquire handler lock success - order_system:reservation_request_handlers
                2025-11-22 11:30:57.852 | 04:30:57.852 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - lock acquire success - order_system:product_reservations
                2025-11-22 11:30:57.853 | 04:30:57.853 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - lock acquire success - order_system:product_availabilities
                2025-11-22 11:30:57.857 | 04:30:57.856 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - get product successfully
                2025-11-22 11:30:57.861 | 04:30:57.860 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - get reservation successfully
                2025-11-22 11:30:57.862 | 04:30:57.862 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - get product availability successfully
                2025-11-22 11:30:57.873 | 04:30:57.872 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - put product_reservation successfully
                2025-11-22 11:30:57.874 | 04:30:57.874 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - put product_availability successfully
                2025-11-22 11:30:57.875 | 04:30:57.875 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - lock release success - order_system:product_availabilities
                2025-11-22 11:30:57.876 | 04:30:57.876 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - lock release success - order_system:product_reservations
                2025-11-22 11:30:57.877 | 04:30:57.877 - DEBUG - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - lock release success - order_system:reservation_request_handlers
                2025-11-22 11:30:57.878 | 04:30:57.877 - INFO  - Reservation - product_id=51a64413-746e-4d03-ae6f-4075764a97b2 - user_id=VU_3_1763785846339_15074 - handler=inventory-service - requested_at=2025-11-22T04:30:57.758859331Z - handle reservation request successfully
                2025-11-22 11:30:57.878 | 04:30:57.878 - INFO  - Reservation - key=VU_3_1763785846339_15074 - partition=0 - group=inventory-service - offset=40648 - hostname=inventory-service - Message committed

Reactor Kafka Experiment
	+ Reproduce the problem
	+ ...
    + ? Instant có được encode/decode đúng hay không

Refactor Inventory Service:
	+ pay attention to which thread the operation is perform too