package thread.marketdata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.google.gson.Gson;

public class RealtimeOrderBookBuilder implements Runnable {
	
	final Logger log = Logger.getLogger("thread.marketdata.RealtimeOrderBookBuilder");
	final Gson gson = new Gson();
	
	RawOrderBookImage initialImage;
	LinkedBlockingQueue<RawOrderBookUpdate> inboundQueue;
	LinkedBlockingQueue<OrderBook> outboundQueue;
	WebSocketClient client;
	MarketDataSocket socket;
	Exchange exchange;
	Products.Product product;
	
	private TreeMap<Double, TreeMap<String, Order>> bidMap; // price -> (order_id -> order)
	private TreeMap<Double, TreeMap<String, Order>> askMap;
	private Integer currentSequence;
	private Instant MarketTime;
	
	RealtimeOrderBookBuilder(Exchange exchange, Products.Product product, 
			LinkedBlockingQueue<OrderBook> outboundQueue) {		
		this.exchange = exchange;
		this.product = product;
		this.outboundQueue = outboundQueue;
	}
	
	void initialize() {
		// Start from a fresh client, socket & queue (could be required if out-of-sequence messages received)
		log.info("Initializing OrderBookBuilder");
		inboundQueue = new LinkedBlockingQueue<RawOrderBookUpdate>();
		bidMap = new TreeMap<Double, TreeMap<String, Order>>();
		askMap = new TreeMap<Double, TreeMap<String, Order>>();

		// Start real-time subscription	filling queue	
		log.info("Starting real time subscription");
		if (! exchange.equals(Exchange.REPLAY)) {
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
		} else {
			Thread t1 = new Thread(new thread.test.DataReplaySocket(product, inboundQueue), "DataReplaySocket");
			t1.start();
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
		
		// Get initial order book image from REST and populate bid/ask maps
		log.info("Getting initial order book image");
		if (! exchange.equals(Exchange.REPLAY)) {
			try {
				URL url = new URL(exchange.level3orderbook);
				MarketTime = Instant.now(); // estimate... not provided in REST
			    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				initialImage = gson.fromJson(in.readLine(), RawOrderBookImage.class);
				log.info("Got initial order book image with sequence number "+initialImage.sequence);
			} catch (Exception e) {
				log.severe("Error when polling order book image: "+e.getMessage());
				e.printStackTrace();
			}
		} else {
			try {
				BufferedReader in = new BufferedReader(new FileReader(exchange.level3orderbook));
				String[] array = in.readLine().split("\t");
				String msg = array[1]; // ignore time-stamp for now
				initialImage = gson.fromJson(msg, RawOrderBookImage.class);
				log.info("Got initial order book image with sequence number "+initialImage.sequence);
				in.close();
			} catch (Exception e) {
				log.severe("Error when reading initial order book image from file: "+e.getMessage());
				e.printStackTrace();
			}
		}
		currentSequence = Integer.parseInt(initialImage.sequence);
		for (String[] array: initialImage.bids) {
			// [price, size, order_id], [price, size, order_id]...
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			String order_id = array[2];
			if (bidMap.containsKey(price)) {
				TreeMap<String, Order> ordersAtLevel = bidMap.get(price);
				// Don't need to check if order already exists: will only appear once in initial snapshot
				ordersAtLevel.put(order_id, new Order(price, size, order_id));
			} else {
				TreeMap<String, Order> ordersAtLevel = new TreeMap<String, Order>();
				ordersAtLevel.put(order_id, new Order(price, size, order_id));
				bidMap.put(price, ordersAtLevel);
			}
			log.finest("Putting order "+order_id+" for "+price.toString()+"x"+size.toString()+" into bid map");
		}
		for (String[] array: initialImage.asks) {
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			String order_id = array[2];
			if (askMap.containsKey(price)) {
				TreeMap<String, Order> ordersAtLevel = askMap.get(price);
				ordersAtLevel.put(order_id, new Order(price, size, order_id));
			} else {
				TreeMap<String, Order> ordersAtLevel = new TreeMap<String, Order>();
				ordersAtLevel.put(order_id, new Order(price, size, order_id));
				askMap.put(price, ordersAtLevel);
			}
			log.finest("Putting order "+order_id+" for "+price.toString()+"x"+size.toString()+" into ask map");
		}
		publish();
		log.info("Completed initialization and published first update");
	}
	
	public void publish() {
		// Sync (private) tree maps to new immutable order book object and emit
		OrderBook ob = new OrderBook(product);
		ob.sequenceNumber = currentSequence;
		ob.MarketTime = MarketTime;
		ob.OrderBookBuilderStartTime = Instant.now();
		Iterator<Double> priceIterator;
		priceIterator = bidMap.descendingKeySet().iterator();
		// Oh lord this is ugly.
		TreeMap<String, Order> ordersAtLevel;
		Iterator<String> orderIterator;
		// Bid0
		ob.BidPrice0 = priceIterator.next();
		ordersAtLevel = bidMap.get(ob.BidPrice0);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			ob.BidSize0 += ord.size;
		}
		// Bid1
		ob.BidPrice1 = priceIterator.next();
		ordersAtLevel = bidMap.get(ob.BidPrice1);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			ob.BidSize1 += ord.size;
		}
		// Bid2
		ob.BidPrice2 = priceIterator.next();
		ordersAtLevel = bidMap.get(ob.BidPrice2);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			ob.BidSize2 += ord.size;
		}
		priceIterator = askMap.keySet().iterator();
		// Ask0
		ob.AskPrice0 = priceIterator.next();
		ordersAtLevel = askMap.get(ob.AskPrice0);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			ob.AskSize0 += ord.size;
		}
		// Ask1
		ob.AskPrice1 = priceIterator.next();
		ordersAtLevel = askMap.get(ob.AskPrice1);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			ob.AskSize1 += ord.size;
		}
		// Ask2
		ob.AskPrice2 = priceIterator.next();
		ordersAtLevel = askMap.get(ob.AskPrice2);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			ob.AskSize2 += ord.size;
		}
		ob.OrderBookBuilderEndTime = Instant.now();
		outboundQueue.add(ob);
	}
	
	public void run() {
		// Start filling queue and get initial REST snapshot (above)
		initialize();
		log.info("Processing delta updates");
		while (! Thread.currentThread().isInterrupted()) {
			// Main loop in normal running
			RawOrderBookUpdate delta;
			try {
				delta = inboundQueue.take();
				if (Integer.parseInt(delta.sequence) <= currentSequence) {
					// Ignore: before our REST snapshot
					log.info("Ignoring delta "+delta.sequence+" as it's before our snapshot");
				} else if ((Integer.parseInt(delta.sequence) == (currentSequence + 1)) 
						&& (delta.product_id.equals(product.id))) {
					// Delta is next in sequence and for correct product: update bid/ask maps
					log.info("Processing delta "+delta.sequence);
					if (delta.type.equals("received")) {
						// An order was received by the matching engine
						// Ignore for now: but we may want to do something with this later for our own orders
					} else if (delta.type.equals("open")) {
						// Add remaining_size to outstanding size on the book
						MarketTime = Instant.parse(delta.time);
						currentSequence = Integer.parseInt(delta.sequence);
						Double price = Double.parseDouble(delta.price);
						Double size = Double.parseDouble(delta.remaining_size);
						/* Order(String type, Instant time, String product_id, Integer sequence, String order_id, String side,
							Double size, Double price, String funds) */
						Order ord = new Order(Order.OrderType.LIMIT, MarketTime, product.id, currentSequence, delta.side,
								delta.order_id, size, price, delta.funds);
						if (delta.side.equals("buy")) {
							// Someone wants to buy, so add to bid map (if they paid more it would have traded)
							// Remember bidMap is price -> (order_id -> order)
							bidMap.get(price);
						}
						publish();
					}
					
					
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
