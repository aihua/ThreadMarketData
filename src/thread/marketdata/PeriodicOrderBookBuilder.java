package thread.marketdata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
	 * Uses TreeMap<Double, Double> (price -> size) for internal representation.
	 * 
	 * In regular mode, waits for PAUSETIME between publications.
	 * In replay mode, uses the time-stamps in the recorded file.
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
	Instant startTime;
	BufferedReader in;
	
	private Integer currentSequence;
	private Instant MarketTime;
	
	PeriodicOrderBookBuilder(Exchange exchange, Products.Product product, LinkedBlockingQueue<OrderBook> outboundQueue) {		
		this.exchange = exchange;
		this.product = product;
		this.outboundQueue = outboundQueue;
		startTime = Instant.now();
	}
	
	public void run() {
		if (exchange.equals(Exchange.REPLAY)) {
			// Only needs one-time initialization if reading from file
			try {
				in = new BufferedReader(new FileReader(exchange.level2orderbook));
				log.info("Opened L2 Order Book file for reading: "+exchange.level2orderbook);
			} catch (FileNotFoundException e) {
				log.severe("Failed to open Level 2 order book from file: "+e.getMessage());
				e.printStackTrace();
			}
		}
		
		while (! Thread.interrupted()) {
			// Main loop: get order book image and interpret
			if (! exchange.equals(Exchange.REPLAY)) {
				try {
					URL url = new URL(exchange.level2orderbook);
					MarketTime = Instant.now(); // estimate... not provided in REST
				    in = new BufferedReader(new InputStreamReader(url.openStream()));
					image = gson.fromJson(in.readLine(), RawOrderBookImage.class);
					log.finest("Got order book image with sequence number "+image.sequence);
				} catch (Exception e) {
					log.severe("Error when polling order book image: "+e.getMessage());
					e.printStackTrace();
				}
			} else {
				try {
					// TODO: This readLine() is going to fail when it gets to the end of the file
					String[] array = in.readLine().split("\t");
					String timeString = array[0];
					String msg = array[1];

					Long currentElapsed = Duration.between(startTime, Instant.now()).toMillis();
					Long targetElapsed = Long.parseLong(timeString);
					if (currentElapsed < targetElapsed){
						Thread.sleep(targetElapsed - currentElapsed);
					}
					MarketTime = Instant.now(); // synthetic time, lined up with expectations
					image = gson.fromJson(msg, RawOrderBookImage.class);
					log.finest("Got order book image with sequence number "+image.sequence);
					
				} catch (Exception e) {
					log.severe("Error when polling order book image: "+e.getMessage());
					e.printStackTrace();
				}
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
			// Time-stamp end and send
			ob.OrderBookBuilderEndTime = Instant.now();
			outboundQueue.add(ob);
			log.info("Outbound order book "+ob.sequenceNumber+" [market->gwy "
					+Duration.between(ob.MarketTime, ob.OrderBookBuilderStartTime).toMillis()+" ms] [gwy processing "
					+Duration.between(ob.OrderBookBuilderStartTime, ob.OrderBookBuilderEndTime).toMillis()+" ms]");
			
			if (! exchange.equals(Exchange.REPLAY)) {
				// Only pause if running from a live server
				try {
					Thread.sleep(PAUSETIME);
				} catch (InterruptedException e) {
					log.severe("Interrupted while pausing PeriodicOrderBookBuilder: "+e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		// finally... dispose of resources gracefully
		try {
			in.close();
		} catch (IOException e) {
			log.severe("Failed to close BufferedReader: "+e.getMessage());
			e.printStackTrace();
		}
	}

}
