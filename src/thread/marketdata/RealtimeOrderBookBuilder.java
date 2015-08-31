package thread.marketdata;

import java.io.BufferedReader;

import thread.common.*;

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

import thread.test.DataReplaySocket;

public class RealtimeOrderBookBuilder implements Runnable {
	
	final Logger log = Logger.getLogger("thread.marketdata.RealtimeOrderBookBuilder");
	final Gson gson = new Gson();
	
	RawOrderBookImage initialImage;
	LinkedBlockingQueue<RawOrderBookUpdate> inboundQueue;
	LinkedBlockingQueue<OrderBook> outboundQueue;
	WebSocketClient client;
	MarketDataSocket socket;
	Exchange exchange;
	String currencyPair;
	
	private TreeMap<Double, TreeMap<String, Order>> bidMap; // price -> (order_id -> order)
	private TreeMap<Double, TreeMap<String, Order>> askMap;
	private Integer currentSequence;
	private Instant MarketTime;
	
	RealtimeOrderBookBuilder(Exchange exchange, String currencyPair, 
			LinkedBlockingQueue<OrderBook> outboundQueue) {		
		this.exchange = exchange;
		this.currencyPair = currencyPair;
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
			socket = new MarketDataSocket(currencyPair, inboundQueue);
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
			Thread t1 = new Thread(new DataReplaySocket(exchange, currencyPair, inboundQueue), "DataReplaySocket");
			t1.start();
		}
		
		// Wait for first update to come through
		try {
			log.info("OrderBookBuilder waiting for first delta update");
			RawOrderBookUpdate ignored = inboundQueue.take();
			log.info("OrderBookBuilder received and ignored first delta update, sequence "+ignored.sequence);
		} catch (InterruptedException e1) {
			log.severe("Interrupted while waiting for first inbound market data update: "+e1.getMessage());
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
			addToMap(bidMap, new Order(price, size, order_id));
		}
		for (String[] array: initialImage.asks) {
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			String order_id = array[2];
			addToMap(askMap, new Order(price, size, order_id));
		}
		publish();
		log.info("Completed initialization and published first update");
	}
	
	public void publish() {
		// Sync (private) tree maps to new immutable order book object and emit
		Integer sequenceNumber = currentSequence;
		Instant OrderBookBuilderStartTime = Instant.now();
		Iterator<Double> priceIterator;
		priceIterator = bidMap.descendingKeySet().iterator();
		// Oh lord this is ugly.
		TreeMap<String, Order> ordersAtLevel;
		Iterator<String> orderIterator;
		// Bid0
		Double BidPrice0 = priceIterator.next();
		Double BidSize0 = 0.0;
		ordersAtLevel = bidMap.get(BidPrice0);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			BidSize0 += ord.size;
		}
		// Bid1
		Double BidPrice1 = priceIterator.next();
		Double BidSize1 = 0.0;
		ordersAtLevel = bidMap.get(BidPrice1);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			BidSize1 += ord.size;
		}
		// Bid2
		Double BidPrice2 = priceIterator.next();
		Double BidSize2 = 0.0;
		ordersAtLevel = bidMap.get(BidPrice2);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			BidSize2 += ord.size;
		}
		priceIterator = askMap.keySet().iterator();
		// Ask0
		Double AskPrice0 = priceIterator.next();
		Double AskSize0 = 0.0;
		ordersAtLevel = askMap.get(AskPrice0);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			AskSize0 += ord.size;
		}
		// Ask1
		Double AskPrice1 = priceIterator.next();
		Double AskSize1 = 0.0;
		ordersAtLevel = askMap.get(AskPrice1);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			AskSize1 += ord.size;
		}
		// Ask2
		Double AskPrice2 = priceIterator.next();
		Double AskSize2 = 0.0;
		ordersAtLevel = askMap.get(AskPrice2);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			AskSize2 += ord.size;
		}
		Instant OrderBookBuilderEndTime = Instant.now();
		OrderBook ob = new OrderBook(currencyPair, MarketTime, OrderBookBuilderStartTime, 
				OrderBookBuilderEndTime, sequenceNumber, BidPrice2, BidSize2, 
				BidPrice1, BidSize1, BidPrice0, BidSize0, 
				AskPrice0, AskSize0, AskPrice1, AskSize1, 
				AskPrice2, AskSize2); 
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
					log.fine("Ignoring delta "+delta.sequence+" as it's before our snapshot");
				} else if ((Integer.parseInt(delta.sequence) == (currentSequence + 1)) 
						&& (delta.product_id.equals(currencyPair))) {
					// Delta is next in sequence and for correct product: update bid/ask maps
					log.fine("Processing delta "+delta.sequence);
					if (delta.type.equals("received")) {
						// An order was received by the matching engine
						// Ignore for now: but we may want to do something with this later for our own orders
						currentSequence++;
						publish();
					} else if (delta.type.equals("open")) {
						// We can safely ignore market (funds) orders, because they trade immediately by definition
	
						// Add remaining_size to outstanding size on the book
						MarketTime = Instant.parse(delta.time);
						currentSequence = Integer.parseInt(delta.sequence);
						Double price = Double.parseDouble(delta.price);
						Double size = Double.parseDouble(delta.remaining_size);
						/* Order(String type, Instant time, String product_id, Integer sequence, String order_id, String side,
							Double size, Double price, String funds) */
						Order ord = new Order(Order.OrderType.LIMIT, MarketTime, currencyPair, currentSequence, 
								delta.order_id, delta.side, size, price, delta.funds);
						if (delta.side.equals("buy")) {
							// Someone wants to buy, so add to bid map (if they paid more it would have traded)
							// Remember bidMap is price -> (order_id -> order)
							addToMap(bidMap, ord);
						} else if (delta.side.equals("sell")) {
							addToMap(askMap, ord);
						}
						publish();
					} else if (delta.type.equals("done")) {
						MarketTime = Instant.parse(delta.time);
						currentSequence = Integer.parseInt(delta.sequence);
						Double price = Double.parseDouble(delta.price);
						if (delta.side.equals("buy")) {
							removeFromMap(bidMap, price, delta.order_id);
						} else if (delta.side.equals("sell")) {
							removeFromMap(askMap, price, delta.order_id);
						}
						publish();
					} else if (delta.type.equals("match")) {
						// Two orders matched, includes distinction of maker/taker
						// Ignore for now (both orders will be "done" anyway) but may be useful later
						currentSequence++;
						publish();
					} else if (delta.type.equals("change")) {
						// Change messages for received, but not yet open, messages can be ignored
						// (caused by self-trade prevention).  This includes by definition all funds messages.
						// Only size can change; it can only go down.
						MarketTime = Instant.parse(delta.time);
						currentSequence = Integer.parseInt(delta.sequence);
						Double price = Double.parseDouble(delta.price);
						if (bidMap.containsKey(price)) { // I think this protects me from a NPE
							// Bit weird I don't have to check side?
							TreeMap<String, Order> ordersAtLevel = bidMap.get(price);
							if (ordersAtLevel.containsKey(delta.order_id)) {
								// It's on the book; it must be a resting limit order
								Order order = ordersAtLevel.get(delta.order_id);
								order.size = Double.parseDouble(delta.new_size);
							}
						} else if (askMap.containsKey(price)) {
							TreeMap<String, Order> ordersAtLevel = askMap.get(price);
							if (ordersAtLevel.containsKey(delta.order_id)) {
								// It's on the book; it must be a resting limit order
								Order order = ordersAtLevel.get(delta.order_id);
								order.size = Double.parseDouble(delta.new_size);
							}
						}
						publish();
					} else if (delta.type.equals("error")) {
						log.severe("Received error message: "+delta.message);
						// The exchange API says it will disconnect us, so let's handle this gracefully
						Thread.currentThread().interrupt();
					}
					
				} else {
					// Missed an inbound sequence number
					log.severe("Received delta update out of sequence, re-initializing");
					
					// TODO: handle if in replay mode
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
	
	private void addToMap(TreeMap<Double, TreeMap<String, Order>> map, Order order) {
		if (map.containsKey(order.price)) {
			TreeMap<String, Order> ordersAtLevel = map.get(order.price);
			if (! ordersAtLevel.containsKey(order.order_id)) {
				ordersAtLevel.put(order.order_id, order);
				log.finest("Added order "+order.toString()+" to existing order map at that price");
			} else {
				log.severe("Attempted to add order "+order.toString()+" to map when it already belongs and failed!");
			}
		} else {
			TreeMap<String, Order> ordersAtLevel = new TreeMap<String, Order>();
			ordersAtLevel.put(order.order_id, order);
			map.put(order.price, ordersAtLevel);
			log.finest("Added order "+order.toString()+" to new order map at that price");
		}
	}
	
	private void removeFromMap(TreeMap<Double, TreeMap<String, Order>> map, Double price, String order_id) {
		if (map.containsKey(price)) {
			TreeMap<String, Order> ordersAtLevel = map.get(price);
			ordersAtLevel.remove(order_id);
			if (ordersAtLevel.isEmpty()) {
				map.remove(price); // for efficiency reasons
			}
		} else {
			log.severe("Attempted to remove order "+order_id+" from map when its price was not present!");
		}
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
