package thread.marketdata;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class MarketData {

	final static Logger log = Logger.getLogger("thread.marketdata.MarketData");
	
	public static void main(String[] args) {
		log.info("Starting up Market Data publisher...");
		// Get products:
		Products p = new Products(Exchange.PRODUCTION);
		log.info("Got products from exchange: "+p.getProducts().toString());
	
		LinkedBlockingQueue<OrderBook> queue = new LinkedBlockingQueue<OrderBook>();
		
		Thread OrderBookBuilderThread = new Thread(
				new PeriodicOrderBookBuilder(Exchange.PRODUCTION, p.products.get(0), queue),"OrderBookBuilderThread");
		OrderBookBuilderThread.start();

		Thread ZeroMQPublisherThread = new Thread(
				new ZeroMQPublisher(queue), "ZeroMQPublisherThread");
		ZeroMQPublisherThread.start();
		

	}

}
