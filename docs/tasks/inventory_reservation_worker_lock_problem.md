- nên cho phép nhiều worker cùng sync, mỗi worker chỉ nên lock 1 batch thôi
  làm sao biết được batch tiếp theo đã được một worker khác sync chưa?

  => dùng kafka, mỗi 5p tạo batch và gửi message đến topic. expiretime của message đó là 5p, vì 5p sau, đằng nào cũng gửi lại


- khi worker đang sync, phải lock hàng loạt product, thay vì như handler chỉ lock một product
  làm sao đảm bảo việc handler sẽ không lock một product nằm trong list của sync?

  => cần dùng lại thư viện LockRepo, lần này dùng HashTable, cho phép lock hàng loạt thay vì chỉ một item
  ? hash table entry có thể có ttl không?

  + cẩn thận với lock ttl, cần gắn dựa trên deadline?

  ⛔ thay vì cố gắng lock tất cả, mỗi lần gọi sẽ acquire nhiều lock nhất có thể
  những lock đã acquire được rồi thì reset ttl
  => nếu vậy 2 process cùng partially acquire lock, không phân biệt được lẫn nhau


	⛔ sort & lock value "from:to"
		=> nghĩa là lock theo range, hay có thể gọi là rangeLock
		=> nhưng acquire lock sẽ phức tạp và **có thể rất chậm** vì phải lặp qua tất cả các range để kiểm tra

 	✅ dùng hashtable + atomic operations:
		- check lock availability: atomic SUM với LUA, nếu SUM = 0, lock available
		- acquire: dùng HMSET -> lock tất cả item trong 1 operation
    - release lock: dùng HDEL
		* các handler/worker nên có gracefully shutdown giữa các operation, tránh việc nó ngay lập tức "tranh giành" lock