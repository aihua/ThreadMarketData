MarketData
----------

Architecture:

A. OrderBookBuilder thread
	1. Send a subscribe message for the product of interest via MarketDataSocket
	2. Queue any messages received over the websocket stream in BlockingQueue
	3. Make a REST request for the order book snapshot from the REST feed.
	4. Play back queued messages, discarding sequence numbers before or equal to the snapshot sequence number.
	5. Apply play back messages to the snapshot as needed.
		i. If I send a "buy" message and some is left over, it's added to the "Asks" side.
		ii. If I send a "sell" message and some is left over, it's added to the "Bids" side.
	6. After play back is complete, apply real-time stream messages as they arrive.
	7. Emit immutable OrderBook messages over a second BlockingQueue to the Zero MQ publisher thread.
B. Zero MQ Publisher thread
	1. Serialize incoming OrderBook message to JSON
	2. Publish on ZeroMQ PUB "socket"

To Do:

* Build test harness for market data using recorded stuff
* (Possibly) Complete real-time Coinbase websocket subscription
* Extend to Bitstamp, or whatever the next largest US exchange is (look at distribution by volume)
* Select exchange/environment and pub/sub IP, port using properties file
* Build test cases - I really ought to do this before I rely on it financially

Issues/Risks:

* Can I do without a full, real-time order book in the short term?
	* Just poll once every few seconds or something
	* Would be enough for low-resolution algos O(days)
	* Would significantly simplify the logic
	* Would be drop-in replaceable for Bitstamp
* RealtimeOrderBookBuilder has "issues"
	* Will Integer x == Integer y work the way I think or do I need to use .equals()?
		* Only if x, y are between -128 and 127 (!!!)
		* https://www.owasp.org/index.php/Java_gotchas#Immutable_Objects_.2F_Wrapper_Class_Caching
		* ...but I look up objects in the TreeMap using Doubles...
		* ...but I pass them by reference... - seems to work for the simpler implementation
		* Probably depends on the precise implementation of TreeMap.containsKey() - does it do .equals() or == ?
	* How do we effectively test the order book algorithm to make sure it works properly?
		* Record a stream of JSON updates in text and build a test framework to check deltas sum up to image
			* Could JUnit be used for this?
			* Include consistency checks, e.g. all orders in bidMap should be bids, etc.
			* Price should increase monotonically
			* Bids should be below asks
			* Market should not be locked, etc.
		* Build good individual unit test cases
	* Should conflation be built in?  If so, where?
		* Dropped updates in the OrderBookBuilder thread are fatal & need resync
		* But we should plan for slow consumers downstream
		* Is this something I have to deal with straight away?
	* Summing over the orders is obviously performance-critical
		* I use Iterators extensively for both inner/outer loops in the OrderBookBuilder, are they OK?
		* Might a simple index over an array be faster?
		* Could I at least apply DRY using the Reflection API?
* Is JSON fast enough for internal serialization, or should I be using Protobuf?
	* Protobuf is complicated & needs separate class definition files
	* Messages are fairly small and infrequent, so it probably doesn't make a diff
	* Keep it simple for now & upgrade to Protobuf if required later

^
|
| P 	Asks: I can buy (i.e. what people ask)
| R	
| I		Best prices; matching
| C	
| E 	Bids: I can sell (i.e. what people bid)
|

Recorded data:

* L3 web socket updates need to start first, at sequence 157,866,615 (if re-recording beware this)
* L3 web service images start afterwards at sequence 157,866,848
