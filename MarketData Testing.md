### Capture the replay (repeatable) data into KDB

Level 3 generated data -> `tick:

Errors:
Sep 06, 2015 6:02:41 PM thread.marketdata.RealtimeOrderBookBuilder removeFromMap
SEVERE: Attempted to remove order f0c3903e-2c80-49b5-a5d6-3ae657375520 from map when its price was not present!
Sep 06, 2015 6:03:11 PM thread.marketdata.RealtimeOrderBookBuilder removeFromMap
SEVERE: Attempted to remove order 22bf2959-c6cc-46a7-be74-481a4e713179 from map when its price was not present!
Sep 06, 2015 6:04:11 PM thread.marketdata.RealtimeOrderBookBuilder removeFromMap
SEVERE: Attempted to remove order f999e8a2-1eb7-41fb-a45a-6f5ddd9d9781 from map when its price was not present!
Sep 06, 2015 6:04:41 PM thread.marketdata.RealtimeOrderBookBuilder removeFromMap
SEVERE: Attempted to remove order 589544d9-3c35-4d07-a02b-b77bd247f729 from map when its price was not present!
Sep 06, 2015 6:05:12 PM thread.marketdata.RealtimeOrderBookBuilder removeFromMap
SEVERE: Attempted to remove order e0f828c8-a62d-4454-adaf-005439b3215d from map when its price was not present!

Level 2 orderbook snapshot data -> `snapl2

No errors (quite simple)

### Perform consistency checking

Check L3 generated <-> L2 orderbook snapshot and identify the point of divergence

Create keyed versions of tables using sequenceNumber, with variant names so we can join:

q)tickKey: select sequenceNumber, l2BidPrice2:BidPrice2, l2BidSize2:BidSize2, l2BidPrice1:BidPrice1, l2BidSize1:BidSize1, l2BidPrice0:BidPrice0, l2BidSize0:BidSize0, l2AskPrice0:AskPrice0, l2AskSize0:AskSize0, l2AskPrice1:AskPrice1, l2AskSize1:AskSize1, l2AskPrice2:AskPrice2, l2AskSize2:AskSize2 from tick
q)`sequenceNumber xkey `tickKey
`tickKey

q)snapl2Key: select sequenceNumber, l2BidPrice2:BidPrice2, l2BidSize2:BidSize2, l2BidPrice1:BidPrice1, l2BidSize1:BidSize1, l2BidPrice0:BidPrice0, l2BidSize0:BidSize0, l2AskPrice0:AskPrice0, l2AskSize0:AskSize0, l2AskPrice1:AskPrice1, l2AskSize1:AskSize1, l2AskPrice2:AskPrice2, l2AskSize2:AskSize2 from snapl2
q)`sequenceNumber xkey `snapl2Key
`snapl2

q)diffs: select BidPrice2Diff:BidPrice2-l2BidPrice2, BidSize2Diff:BidSize2-l2BidSize2, BidPrice1Diff:BidPrice1-l2BidPrice1, BidSize1Diff:BidSize1-l2BidSize1, BidPrice0Diff:BidPrice0-l2BidPrice0, BidSize0Diff:BidSize0-l2BidSize0, AskPrice0Diff:AskPrice0-l2AskPrice0, AskSize0Diff:AskSize0-l2AskSize0, AskPrice1Diff:AskPrice1-l2AskPrice1, AskSize1Diff:AskSize1-l2AskSize1, AskPrice2Diff:AskPrice2-l2AskPrice2, AskSize2Diff:AskSize2-l2AskSize2 from snapl2Key ij tickKey


### Ignore all of that

What we're going to do is this:

* Setup 1
	* Run a regular Level 3 order book market data model
	* Capture everything to KDB
* Setup 2
	* Capture the full order book to test
	* 

