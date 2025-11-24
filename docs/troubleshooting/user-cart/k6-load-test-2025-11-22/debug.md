2025-11-22 09:56:07.901 | 02:56:07.900 - INFO  - c.e.i.m.m.r.ReservationsListener - key=VU_1_1763780167774_38842 - partition=0 - group=inventory-service-1 - offset=40475 - hostname=inventory-service-1 - Message received
2025-11-22 09:56:07.901 | 02:56:07.901 - INFO  - Reservation - product_id=b7d0f2f0-6f5b-4317-bdd5-e675c1eda4c7 - user_id=VU_1_1763780167774_38842 - handler=inventory-service-1 - handling reservation request
2025-11-22 09:56:07.952 | 02:56:07.952 - ERROR - Reservation - product_id=b7d0f2f0-6f5b-4317-bdd5-e675c1eda4c7 - user_id=VU_1_1763780167774_38842 - handler=inventory-service-1 - request validation failed - invalid request timestamp
2025-11-22 09:56:07.958 | 02:56:07.958 - INFO  - c.e.i.m.m.r.ReservationsListener - key=VU_1_1763780167774_38842 - partition=0 - group=inventory-service-1 - offset=40475 - hostname=inventory-service-1 - Message committed
2025-11-22 09:56:08.085 | 02:56:08.083 - INFO  - c.e.i.m.m.r.ReservationsListener - key=VU_2_1763780167774_38842 - partition=0 - group=inventory-service-1 - offset=40476 - hostname=inventory-service-1 - Message received
2025-11-22 09:56:08.086 | 02:56:08.083 - INFO  - Reservation - product_id=4ac4a906-4510-41fe-88ec-72d8c73964fe - user_id=VU_2_1763780167774_38842 - handler=inventory-service-1 - handling reservation request
2025-11-22 09:56:08.099 | 02:56:08.099 - ERROR - Reservation - product_id=4ac4a906-4510-41fe-88ec-72d8c73964fe - user_id=VU_2_1763780167774_38842 - handler=inventory-service-1 - try acquire handler lock failed - lock unavailable - collection: order-system:locks:order_system:reservation_request_handlers
2025-11-22 09:56:08.105 | 02:56:08.104 - INFO  - c.e.i.m.m.r.ReservationsListener - key=VU_2_1763780167774_38842 - partition=0 - group=inventory-service-1 - offset=40476 - hostname=inventory-service-1 - Did not commit message

	1 total request handled ở backend nhỏ hơn k6
	2 try acquire handler lock failed -> lẽ ra nên commit message luôn
	3 tại sao ko commit message mà tiếp tục nhận message mới, thay vì redeliver

message receive: 
	inven-1:
		total received: 108
		total-err: 66 (handler-lock-fail-err: 43, timestamp-err: 23)
		success: 40
		=> sum is 106 -> missing 2 requests??
			2025-11-22 09:56:15.001 | 02:56:15.000 - INFO  - c.e.i.m.m.r.ReservationsListener - key=VU_3_1763780167774_38842 - partition=0 - group=inventory-service-1 - offset=			40568 - hostname=inventory-service-1 - Message received
			2025-11-22 09:56:15.001 | 02:56:15.001 - INFO  - Reservation - product_id=e3b8b8f0-cfce-45ee-acbe-e8a359f57822 - user_id=VU_3_1763780167774_38842 - handler=inventory-			service-1 - handling reservation request
			2025-11-22 09:56:15.052 | 02:56:15.052 - INFO  - c.e.i.m.m.r.ReservationsListener - key=VU_3_1763780167774_38842 - partition=0 - group=inventory-service-1 - offset=
			40568 - hostname=inventory-service-1 - Message committed
			=> No error, but no success log, and committed

	inven:
		total received: 108		
		total-err: 51 (handler-lock-fail-err: 38, timestamp-err: 13)
		success: 57
		=> sum is 108: ok

