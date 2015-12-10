package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import main.sockets.DataReplaySocket;
import main.sockets.MarketDataSocket;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.google.gson.Gson;

public class RealtimeOrderBookBuilder implements Runnable {
	
	final Logger log = Logger.getLogger("main.RealtimeOrderBookBuilder");
	final Gson gson = new Gson();
	
	LinkedBlockingQueue<RawOrderBookUpdate> inboundQueue;
	LinkedBlockingQueue<OrderBook> outboundQueue;
	WebSocketClient client;
	MarketDataSocket socket;
	Exchange exchange;
	String currencyPair;
	private OrderMap om;
	
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
				// Live
				URL url = new URL(exchange.level3orderbook);
			    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				RawOrderBookImage initialImage = gson.fromJson(in.readLine(), RawOrderBookImage.class);
				om = new OrderMap(currencyPair, initialImage, Integer.parseInt(initialImage.sequence), Instant.now());
				log.info("Created new OrderMap with sequence number "+initialImage.sequence);
			} catch (Exception e) {
				log.severe("Error when polling order book image: "+e.getMessage());
				e.printStackTrace();
			}
		} else {
			try {
				// Read from file
				BufferedReader in = new BufferedReader(new FileReader(exchange.level3orderbook));
				String[] array = in.readLine().split("\t");
				String msg = array[1]; // ignore recorded time-stamp for now
				RawOrderBookImage initialImage = gson.fromJson(msg, RawOrderBookImage.class);
				om = new OrderMap(currencyPair, initialImage, Integer.parseInt(initialImage.sequence), Instant.now());
				log.info("Got initial order book image with sequence number "+initialImage.sequence);
				in.close();
			} catch (Exception e) {
				log.severe("Error when reading initial order book image from file: "+e.getMessage());
				e.printStackTrace();
			}
		}

		outboundQueue.add(om.emitOrderBook());
		log.info("Completed initialization and published first update");
	}
	
	public void run() {
		// Start filling queue and get initial REST snapshot (above)
		initialize();
		log.info("Processing delta updates");
		RawOrderBookUpdate delta = null;
		while (! Thread.currentThread().isInterrupted()) {
			// Main loop in normal running
			try {
				delta = inboundQueue.take();
				if (Integer.parseInt(delta.sequence) <= om.getSequence()) {
					// Ignore: before our REST snapshot
					log.fine("Ignoring RealtimeEvent "+delta.sequence+" as it's before our snapshot");
				} else if (Integer.parseInt(delta.sequence) == (om.getSequence() + 1) && 
						(delta.product_id.equals(currencyPair))) {
					// Delta is next in sequence and for correct product: update bid/ask maps
					log.fine("Processing event "+delta.sequence);
					if (delta.type.equals("received")) {
						// An order was received by the matching engine
						// Ignore for now: but we may want to do something with this later for our own orders
						Instant MarketTime = Instant.parse(delta.time);
						om.setSequence(Integer.parseInt(delta.sequence));
						om.setMarketTime(MarketTime);
						outboundQueue.add(om.emitOrderBook());
					} else if (delta.type.equals("open")) {
						// Market (funds) orders are never "open" because they trade immediately by definition
						// Add remaining_size to outstanding size on the book
						Instant MarketTime = Instant.parse(delta.time);
						Integer currentSequence = Integer.parseInt(delta.sequence);
						Double price = Double.parseDouble(delta.price);
						Double size = Double.parseDouble(delta.remaining_size);
						Order ord = new Order(Order.OrderType.LIMIT, currencyPair, delta.order_id, delta.side, size, price);
						if (delta.side.equals("buy")) {
							// Someone wants to buy, so add to bid map (if they paid more it would have traded)
							// Remember bidMap is price -> (order_id -> order)
							om.addToMap("bid", ord);
							om.setSequence(currentSequence);
							om.setMarketTime(MarketTime);
						} else if (delta.side.equals("sell")) {
							om.addToMap("ask", ord);
							om.setSequence(currentSequence);
							om.setMarketTime(MarketTime);
						}
						outboundQueue.add(om.emitOrderBook());
					} else if (delta.type.equals("done")) {
						Instant MarketTime = Instant.parse(delta.time);
						Integer currentSequence = Integer.parseInt(delta.sequence);
						Double price = Double.parseDouble(delta.price);
						if (delta.side.equals("buy")) {
							om.removeFromMap("bid", price, delta.order_id);
							om.setSequence(currentSequence);
							om.setMarketTime(MarketTime);
						} else if (delta.side.equals("sell")) {
							om.removeFromMap("ask", price, delta.order_id);
							om.setSequence(currentSequence);
							om.setMarketTime(MarketTime);
						}
						outboundQueue.add(om.emitOrderBook());
					} else if (delta.type.equals("match")) {
						// Two orders matched, includes distinction of maker/taker
						// Ignore for now (both orders will be "done" anyway) but may be useful later
						Instant MarketTime = Instant.parse(delta.time);
						Integer currentSequence = Integer.parseInt(delta.sequence);
						om.setSequence(currentSequence);
						om.setMarketTime(MarketTime);
						outboundQueue.add(om.emitOrderBook());
					} else if (delta.type.equals("change")) {
						// Change messages for received, but not yet open, messages can be ignored
						// (caused by self-trade prevention).  This includes by definition all funds messages.
						// Only size can change; it can only go down.
						Instant MarketTime = Instant.parse(delta.time);
						Integer sequenceNumber = Integer.parseInt(delta.sequence);
						Double price = Double.parseDouble(delta.price);
						Double newSize = Double.parseDouble(delta.new_size);
						Double newFunds = Double.parseDouble(delta.new_funds);
						om.changeInMap(price, delta.order_id, newSize, newFunds);
						om.setMarketTime(MarketTime);
						om.setSequence(sequenceNumber);
						outboundQueue.add(om.emitOrderBook());
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
				log.severe("Caught InterruptedException while trying to take from inbound queue: delta "+delta.toString());
				e.printStackTrace();
			} catch (Exception e) {
				log.severe("Caught exception trying to take from inbound queue: delta "+delta.toString());
				e.printStackTrace();
			}
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
