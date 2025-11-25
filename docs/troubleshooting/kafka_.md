messsage out of order: need research kafka

        Current setting: 1. auto-commit: false 2. ack-mode: manual_immediate 3. max-poll-records: 1 I am expecting the following behaviors: - only one message per poll - the listener function will trigger a reactor Mono to handle the message and return immediately, but it will not ack the message. The Mono must decide to ack or not. - if the Mono did not ack, it want to re-process the message when the listener got the message re-deliver by the poll() But currently: - if the Mono did not ack, the poll() give the next message instead of redeliver


2025-11-25T09:39:32.756+07:00 DEBUG 9472 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Committing: {kafka-demo-0=OffsetAndMetadata{offset=4, leaderEpoch=null, metadata=''}}



2025-11-25T09:36:04.620+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] .a.RecordMessagingMessageListenerAdapter : Async result is null, ignoring
2025-11-25T09:36:09.631+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records
2025-11-25T09:36:14.645+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records
2025-11-25T09:36:19.652+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records
2025-11-25T09:36:24.661+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records
2025-11-25T09:36:29.673+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records
2025-11-25T09:36:34.686+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records
2025-11-25T09:36:39.698+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records
2025-11-25T09:36:44.714+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records
2025-11-25T09:36:49.723+07:00 DEBUG 6264 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : Received: 0 records


Khi ko gọi ack():
+ nếu trả về Mono: Spring tự động commit mà không tôn trọng configuration
+ nếu là blocking: Spring sẽ không commit.  ⭐

How long until a rebalance if not commit, or only when restart, rebalance happens
=> only on restart ⭐
=> nhưng nếu gọi .nack(duration) thì có thể ko cần chờ restart, mà tự động sau "duration"
note: nack() can only be called on the consumer thread)

Nếu không trả về Mono, mà gọi .block() thì sao:
=> multithread vẫn chạy tốt, chỉ có thread của listner là bị block ⭐

Để chỉ handle một message một lần, có nhất thiết phải max-poll-records=1 không?
  => không cần, có vẻ ở chế độ "MANUAL", thì nếu chưa ack(), sẽ không có message mới

Reactor Kafka Experiment
+ Reproduce the problem
+ ...

Poll():
+ quan sát thấy Mỗi 5s, nó poll một lần thì phải
nhưng ngay khi gửi message, dù chưa đến 5s thì ngay lập tức message được nhận bởi Listener
? - ChatGPT: log 5s đó là health check gì đó, còn thực tế nó poll liên tục (mỗi 100ms)

exeption handling:
+ listner error handler: retry/backoff & DLP
+ default:
nó sẽ thực hiện retry cho đến khi đạt backoff limit thì nó "2025-11-25T11:23:01.221+07:00 DEBUG 20440 --- [ntainer#0-0-C-1] o.s.kafka.listener.DefaultErrorHandler   : Skipping seek of: kafka-demo-0@13"
=> nghĩa là khi bị error, nó sẽ retry, nếu ko được thì nó sẽ bỏ qua offset này. nhưng nó ko commit, nên khi restart, mình sẽ nhận lại message đó
nếu như sau đó mà có thực hiện commit một message khác thì message bị fail trước đó sẽ bị mất, vì bản chất commit là xác nhận offset mới.
cách để ko mất message là đưa nó vào DLP



	