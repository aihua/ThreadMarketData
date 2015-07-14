package thread.marketdata;

import java.util.concurrent.LinkedBlockingQueue;

public class RedisPublisher implements Runnable {
	// https://github.com/xetorthio/jedis/wiki
	
	LinkedBlockingQueue<OrderBook> inboundQueue;
	
	RedisPublisher(LinkedBlockingQueue<OrderBook> inboundQueue) {
		this.inboundQueue = inboundQueue;
	}

	public void run() {
		// TODO Auto-generated method stub
		
	}
}
