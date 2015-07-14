package thread.marketdata;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.google.gson.Gson;

public class OrderBookBuilder implements Runnable {
	
	final Logger log = Logger.getLogger("thread.marketdata.OrderBookBuilder");
	final Gson gson = new Gson();
	
	RawOrderBookImage initialImage;
	LinkedBlockingQueue<RawOrderBookUpdate> inboundQueue;
	LinkedBlockingQueue<OrderBook> outboundQueue;
	WebSocketClient client;
	MarketDataSocket socket;
	Exchange exchange;
	Products.Product product;
	TreeMap<Double, Double> bidMap;
	TreeMap<Double, Double> askMap;
	Integer currentSequence;
	
	OrderBookBuilder(Exchange exchange, Products.Product product) {		
		this.exchange = exchange;
		this.product = product;
	}
	
	void initialize() {
		// Start from a fresh client, socket & queue (could be required if out-of-sequence messages received)
		log.info("Initializing OrderBookBuilder");
		inboundQueue = new LinkedBlockingQueue<RawOrderBookUpdate>();
		outboundQueue = new LinkedBlockingQueue<OrderBook>();
		bidMap = new TreeMap<Double, Double>();
		askMap = new TreeMap<Double, Double>();

		// Start real-time subscription	filling queue	
		log.info("Starting real time subscription");
		client = new WebSocketClient(new SslContextFactory());
		socket = new MarketDataSocket(product, inboundQueue);
		try {
			client.start();
			URI uri = new URI(exchange.websocket);
			log.info("Connecting to:"+uri.toString());
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			client.connect(socket, uri, request);
		} catch (Exception e) {
			log.severe("Caught exception opening websocket: "+e.getMessage());
			e.printStackTrace();
		}
		
		// Wait for first update to come through
		try {
			log.info("OrderBookBuilder waiting for first delta update");
			RawOrderBookUpdate ignored = inboundQueue.take();
			log.info("OrderBookBuilder received and ignored first delta update, sequence "+ignored.sequence);
		} catch (InterruptedException e1) {
			log.severe("Interrupted while waiting for first inbound market data update");
			e1.printStackTrace();
		}
		
		// Get initial order book image from webservice and populate bid/ask maps
		log.info("Getting initial order book image");
		try {
			URL url = new URL(exchange.API + "/products/" + product.id + "/book?level=2");
		    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			initialImage = gson.fromJson(in.readLine(), RawOrderBookImage.class);
			log.info("Got initial order book image with sequence number "+initialImage.sequence);
		} catch (Exception e) {
			log.severe("Error when polling order book image: "+e.getMessage());
			e.printStackTrace();
		}
		currentSequence = Integer.parseInt(initialImage.sequence);
		for (String[] array: initialImage.bids) {
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			// Ignore array[2], num-orders
			bidMap.put(price, size);
			log.finest("Putting px "+price.toString()+"x"+size.toString()+" into bid map");
		}
		for (String[] array: initialImage.asks) {
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			// Ignore array[2], num-orders
			askMap.put(price, size);
			log.finest("Putting px "+price.toString()+"x"+size.toString()+" into ask map");
		}
		publish();
		log.info("Completed initialization and published first update");
	}
	
	public void publish() {
		OrderBook o = new OrderBook(product);
		o.sequenceNumber = currentSequence;
		Iterator<Double> i;
		i = bidMap.descendingKeySet().iterator();
		o.BidPrice0 = i.next();
		o.BidSize0 = bidMap.get(o.BidPrice0);
		o.BidPrice1 = i.next();
		o.BidSize1 = bidMap.get(o.BidPrice1);
		o.BidPrice2 = i.next();
		o.BidSize2 = bidMap.get(o.BidPrice2);
		i = askMap.keySet().iterator();
		o.AskPrice0 = i.next();
		o.AskSize0 = askMap.get(o.AskPrice0);
		o.AskPrice1 = i.next();
		o.AskSize1 = askMap.get(o.AskPrice1);
		o.AskPrice2 = i.next();
		o.AskSize2 = askMap.get(o.AskPrice2);
		outboundQueue.add(o);
	}
	
	public void run() {
		// Start filling queue and get initial market data image
		initialize();
		log.info("Processing delta updates");
		while (! Thread.currentThread().isInterrupted()) {
			// Main loop in normal running
			RawOrderBookUpdate delta;
			try {
				delta = inboundQueue.take();
				if (Integer.parseInt(delta.sequence) <= currentSequence) {
					// Ignore: before our snapshot
					log.info("Ignoring delta "+delta.sequence+" as it's before our snapshot");
				} else if (Integer.parseInt(delta.sequence) == (currentSequence + 1)) {
					// Delta is next in sequence: update bid/ask maps
					log.info("Processing delta "+delta.sequence);
					
					
					publish();
					// Processing complete and update published
					currentSequence++;
				} else {
					// Missed an inbound sequence number
					log.severe("Received delta update out of sequence, re-initializing");
					client.stop();
					initialize();
				}
			} catch (InterruptedException e) {
				log.severe("Caught InterruptedException while trying to take from inbound queue");
				e.printStackTrace();
			} catch (Exception e) {
				log.severe("Caught exception trying to stop websocket client gracefully: "+e.getMessage());
				e.printStackTrace();
			}
			

		}
		
		// finally... dispose of resources gracefully?
		
	}
	
	void shutdown() {
		try {
			client.stop();
		} catch (Exception e) {
			log.severe("Caught exception on shutdown: "+e.getMessage());
			e.printStackTrace();
		}
	}
}
