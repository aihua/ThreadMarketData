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

* Extend to Bitstamp, or whatever the next largest US exchange is (look at distribution by volume)
* Build test cases - I really ought to do this before I rely on it financially

Useful:
* Print latency report of last 100 ticks in DB:
select count sequenceNumber by 1 xbar timeDiff:(KDBCaptureTime-OrderBookBuilderStartTime)*(24*60*60*1000) from (select [-100] from tick)

Issues/Risks:

* RealtimeOrderBookBuilder has "issues"
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
* Is JSON fast enough for internal serialization over ZeroMQ, or should I be using Protobuf?
	* Protobuf is complicated & needs separate class definition files
	* Messages are fairly small and infrequent, so it probably doesn't make a diff
	* Keep it simple for now & upgrade to Protobuf if required later
	* The calculated L2 order books are pretty compact though

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

KDB setup
---------

* Database should be splayed (each col in diff file) and partitioned (by date), e.g.
	`:db/2015.11.11/t/ set ([] time:09:30:00 09:31:00 sym:`:db/sym?`ibm`msft; p:101 33f)
* Q can automatically load the database and any other serialised objects if path is specified on the command line
* The page you want to follow is http://code.kx.com/wiki/JB:KdbplusForMortals/splayed_tables et seq
* In order:
	* Create the table, e.g. `:db/t/ set ([] ti:09:30:00 09:31:00; p:101.5 33.5)
		* Note no leading / (not root dir) but there is a trailing slash (to indicate splayed table)
	* "Load" (tie) the database into namespace, e.g. \l db
		* Note no slashes
	* Or just start q pointing to the db, e.g. q db/
		* Not sure if trailing slash changes behavior (perhaps)
	* Upsert into the table, e.g. `:db/t/ upsert(09:32:00; 102.5)
		* Note Q is annoying about datatypes; 102 wouldn't work (102f or 102.0 would)
	* Syms in tables must be enumerated in db/sym, and the quickest and easiest way to do this is using .Q.en, e.g.
		* memtab:([]sym:`APPL`MSFT`GOOG;px:605.0 75.2 405.2)
		* `:db/disktab/ set .Q.en[`:db] memtab
* 

