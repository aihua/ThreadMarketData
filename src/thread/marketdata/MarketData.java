package thread.marketdata;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class MarketData {

	final static Logger log = Logger.getLogger("thread.marketdata.MarketData");
	
	public static void main(String[] args) {
		// Get products:
		Products p = new Products(Exchange.PRODUCTION);
		log.info("Got products from exchange: "+p.getProducts().toString());
	
		LinkedBlockingQueue<OrderBook> queue = new LinkedBlockingQueue<OrderBook>();
		
		Thread OrderBookBuilderThread = new Thread(
				new OrderBookBuilder(Exchange.PRODUCTION, p.products.get(0), queue),"OrderBookBuilderThread");
		OrderBookBuilderThread.start();

		Thread RedisPublisherThread = new Thread(new RedisPublisher(queue), "RedisPublisherThread");
		RedisPublisherThread.start();
	}

}
