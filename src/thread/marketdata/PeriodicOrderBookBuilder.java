package thread.marketdata;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.google.gson.Gson;

public class PeriodicOrderBookBuilder implements Runnable {
	
	/* Generates outbound order books in about 1 ms on Macbook Pro, July 2015.
	 * Uses TreeMap<Double, Double> (price -> size) for internal representation
	 */
	
	final static Integer PAUSETIME = 5000;
	
	final Logger log = Logger.getLogger("thread.marketdata.PeriodicOrderBookBuilder");
	final Gson gson = new Gson();
	
	RawOrderBookImage image;
	LinkedBlockingQueue<OrderBook> outboundQueue;
	Exchange exchange;
	Products.Product product;
	TreeMap<Double, Double> bidMap;
	TreeMap<Double, Double> askMap;
	
	private Integer currentSequence;
	private Instant MarketTime;
	
	PeriodicOrderBookBuilder(Exchange exchange, Products.Product product, LinkedBlockingQueue<OrderBook> outboundQueue) {		
		this.exchange = exchange;
		this.product = product;
		this.outboundQueue = outboundQueue;
	}
	
	public void run() {
		while (! Thread.currentThread().isInterrupted()) {
			// Get order book image from REST and Java-ify
			try {
				URL url = new URL(exchange.API + "/products/" + product.id + "/book?level=2");
				MarketTime = Instant.now(); // estimate... not provided in REST
			    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				image = gson.fromJson(in.readLine(), RawOrderBookImage.class);
				log.info("Got order book image with sequence number "+image.sequence);
			} catch (Exception e) {
				log.severe("Error when polling order book image: "+e.getMessage());
				e.printStackTrace();
			}
			OrderBook ob = new OrderBook(product);
			// Time-stamp start and populate TreeMaps
			ob.OrderBookBuilderStartTime = Instant.now();
			ob.MarketTime = MarketTime;
			currentSequence = Integer.parseInt(image.sequence);
			ob.sequenceNumber = currentSequence;
			bidMap = new TreeMap<Double, Double>();
			askMap = new TreeMap<Double, Double>();
			for (String[] array: image.bids) {
				// [ price, size, num-orders ], [ price, size, num-orders ]...
				Double price = Double.parseDouble(array[0]);
				Double size = Double.parseDouble(array[1]);
				// Ignore num-orders
				bidMap.put(price, size);
				
			}
			for (String[] array: image.asks) {
				// [ price, size, num-orders ], [ price, size, num-orders ]...
				Double price = Double.parseDouble(array[0]);
				Double size = Double.parseDouble(array[1]);
				// Ignore num-orders
				askMap.put(price, size);
			}
			Iterator<Double> priceIterator;
			priceIterator = bidMap.descendingKeySet().iterator();
			ob.BidPrice0 = priceIterator.next();
			ob.BidSize0 = bidMap.get(ob.BidPrice0);
			ob.BidPrice1 = priceIterator.next();
			ob.BidSize1 = bidMap.get(ob.BidPrice1);
			ob.BidPrice2 = priceIterator.next();
			ob.BidSize2 = bidMap.get(ob.BidPrice2);
			priceIterator = askMap.keySet().iterator();
			ob.AskPrice0 = priceIterator.next();
			ob.AskSize0 = askMap.get(ob.AskPrice0);
			ob.AskPrice1 = priceIterator.next();
			ob.AskSize1 = askMap.get(ob.AskPrice1);
			ob.AskPrice2 = priceIterator.next();
			ob.AskSize2 = askMap.get(ob.AskPrice2);
			// Timestamp end and send
			ob.OrderBookBuilderEndTime = Instant.now();
			outboundQueue.add(ob);
			log.info("Outbound order book "+ob.sequenceNumber+" [market->gwy "
					+Duration.between(ob.MarketTime, ob.OrderBookBuilderStartTime).toMillis()+" ms] [gwy processing "
					+Duration.between(ob.OrderBookBuilderStartTime, ob.OrderBookBuilderEndTime).toMillis()+" ms]");
			
			try {
				Thread.sleep(PAUSETIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// finally... dispose of resources gracefully?
		
	}

}
