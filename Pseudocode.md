MarketData
----------

High level approach:

1. Send a subscribe message for the product of interest.
2. Queue any messages received over the websocket stream.
3. Make a REST request for the order book snapshot from the REST feed.
4. Playback queued messages, discarding sequence numbers before or equal to the snapshot sequence number.
5. Apply playback messages to the snapshot as needed (see below).
6. After playback is complete, apply real-time stream messages as they arrive.
