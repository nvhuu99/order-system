# [Design] Cart Service Handler - Distribute the request evenly

## First Approach:

- Use "Round Robin" algorithm:
  - Pros: No need to keep tracks of the total workers (memberlist)
  - Cons: Need another service that handle the distributions (a Manager/Distributor)

- Use "Hash-based" algorithm:
	+ Pros: each workers can run the hash and decide without being dependent to a Manager.
	+ Cons: Need to keep tracks of the total workers

- **Problem #1:** even when all the user-carts distributed evenly, the fact that any user session can end at anytime still create imbalance.

- **Problem #2:** each time a worker shutdown, it need to release all the locks. Right that moment, other workers will take over the items, leading to future imbalance.

## Second Approach

- But kafka only handle the distribution with topic-partitions, why "re-invent the wheel". Just create separate consumer-groups with @KafkaListener annotation.

- **Problem:** Kafka can not guarantee evenly distribution. It's only guarantee one partition per key.

## Third Approach:

- Dont lock if the version mismatch.


	c1.checkVer() -> ok
		c1.acquireLock -> ok
			c1.handle -> ok
				c1.release
					c1.commit
					

		c1.acquireLock -> fail
			c1.commit			-> same problem before

	c1.checkVer() -> fail
		c1.commit				-> if you are ahead -> skip
							   if you are before -> skip
							   only when match -> handle -> Solve the above problem