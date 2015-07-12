package thread.marketdata;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.google.gson.Gson;

public class OrderBookBuilder {
	
	final Logger log = Logger.getLogger("thread.marketdata.OrderBookBuilder");
	final Gson gson = new Gson();
	Queue<RawOrderBookUpdate> queue;
	WebSocketClient client;
	MarketDataSocket socket;
	
	OrderBookBuilder(Exchange exchange, Products.Product product) {
		
		log.info("Creating OrderBookBuilder");
		queue = new ConcurrentLinkedQueue<RawOrderBookUpdate>();

		// Start real-time subscription		
		client = new WebSocketClient(new SslContextFactory());
		socket = new MarketDataSocket(product, queue);
		try {
			client.start();
			URI uri = new URI(Exchange.PRODUCTION.websocket);
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
		Gson gson = new Gson();
		try {
			URL url = new URL(exchange.API + "/products/" + product.id + "/book?level=2");
		    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			RawOrderBookImage r = gson.fromJson(in.readLine(), RawOrderBookImage.class);
			log.info("Got RawOrderBookImage with sequence number "+r.sequence);
		} catch (Exception e) {
			log.severe("Error: "+e.getMessage());
			e.printStackTrace();
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
