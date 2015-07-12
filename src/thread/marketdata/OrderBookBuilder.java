package thread.marketdata;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.google.gson.Gson;

public class OrderBookBuilder {
	
	final Logger log = Logger.getLogger("thread.marketdata.OrderBookBuilder");
	final Gson gson = new Gson();
	
	RawOrderBookImage r;
	Queue<RawOrderBookUpdate> queue;
	WebSocketClient client;
	MarketDataSocket socket;
	Exchange exchange;
	Products.Product product;
	OrderBook o;
	TreeMap<Double, Double> bidMap;
	TreeMap<Double, Double> askMap;
	
	OrderBookBuilder(Exchange exchange, Products.Product product) {		
		this.exchange = exchange;
		this.product = product;
	}
	
	void initialize() {
		// Start from a fresh order book/queue (could be required if out-of-sequence messages received)
		log.info("Initializing OrderBookBuilder");
		queue = new LinkedBlockingQueue<RawOrderBookUpdate>();
		o = new OrderBook(product);
		bidMap = new TreeMap<Double, Double>();
		askMap = new TreeMap<Double, Double>();

		// Start real-time subscription	filling queue	
		log.info("Starting real time subscription");
		client = new WebSocketClient(new SslContextFactory());
		socket = new MarketDataSocket(product, queue);
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
		
		// Wait a bit until connection starts publishing data
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			log.severe("Caught InterruptedException while waiting for MarketDataSocket to initialize: "+e1.getMessage());
			e1.printStackTrace();
		}
		
		// Get order book image
		log.info("Getting order book image");
		try {
			URL url = new URL(exchange.API + "/products/" + product.id + "/book?level=2");
		    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			r = gson.fromJson(in.readLine(), RawOrderBookImage.class);
			log.info("Got RawOrderBookImage with sequence number "+r.sequence);
		} catch (Exception e) {
			log.severe("Error when polling order book image: "+e.getMessage());
			e.printStackTrace();
		}

	}
	
	void run() {
		// Start filling queue and get initial market data image
		initialize();
		
		// Build TreeMap from initial RawOrderBookImage
		o.sequenceNumber = Integer.parseInt(r.sequence);
		for (String[] array: r.bids) {
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			// Ignore array[2], num-orders
			bidMap.put(price, size);
			//log.info("Putting px "+price.toString()+"x"+size.toString()+" into bid map");
		}
		for (String[] array: r.asks) {
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			// Ignore array[2], num-orders
			askMap.put(price, size);
			//log.info("Putting px "+price.toString()+"x"+size.toString()+" into ask map");
		}
		// Build OrderBook from TreeMap
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
		log.info("Built initial OrderBook "+o.toString());
		
		// Process queue
		
		
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
