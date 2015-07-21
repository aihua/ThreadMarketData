package thread.test;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import thread.marketdata.*;

public class RealtimeDataCapture {
	// Used to capture data from the webservices to disk for later coherence testing
	
	final static Logger log = Logger.getLogger("thread.marketdata.RealtimeDataCapture");
	
	public static void main (String[] args) {
		
		log.info("Initializing RealtimeDataCapture");
		
		Thread t1 = new Thread() {
			public void run() {
				Instant startTime = Instant.now();
				Products p = new Products(Exchange.PRODUCTION);
				LinkedBlockingQueue<String> inboundQueue = new LinkedBlockingQueue<String>();

				// Open output file
				PrintWriter out = null;
				try {
					FileWriter fw = new FileWriter("/Users/nick/Dev/Data/Coinbase-websocket-updates.txt");
					out = new PrintWriter(fw);
				} catch (Exception e) {
					log.severe("Failed to open file for writing: "+e.getMessage());
					e.printStackTrace();
				}
				
				// Start real-time subscription	filling queue	
				log.info("Starting real time subscription");
				WebSocketClient client = new WebSocketClient(new SslContextFactory());
				DataCaptureSocket socket = new DataCaptureSocket(p.getProducts().get(0), inboundQueue);
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
				
				// Infinite loop
				while (! Thread.interrupted()) {
					try {	
						String msg = inboundQueue.take();
						Instant timestamp = Instant.now();
						out.println(Duration.between(startTime, timestamp).toMillis()+"\t"+msg);
						log.info("Processed websocket update");
					} catch (Exception e1) {
						log.severe("Interrupted while waiting for  inbound market data update");
						e1.printStackTrace();
					}
				}
				
				out.close();
			}
		};
		t1.start();
		
		Thread t2 = new Thread() {
			public void run() {
				Instant startTime = Instant.now();
				Products p = new Products(Exchange.PRODUCTION);
				Products.Product product = p.getProducts().get(0);
				
				// Open output file
				PrintWriter out = null;
				try {
					FileWriter fw = new FileWriter("/Users/nick/Dev/Data/Coinbase-webservice-images.txt");
					out = new PrintWriter(fw);
					log.info("Opened output file for webservice images");
				} catch (Exception e) {
					log.severe("Failed to open file for writing: "+e.getMessage());
					e.printStackTrace();
				}
		
				while (! Thread.currentThread().isInterrupted()) {
					try {
						URL url = new URL(Exchange.PRODUCTION.API + "/products/" + product.id + "/book?level=2");
						Instant timestamp = Instant.now(); // estimate... not provided in REST
					    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
						String msg = in.readLine();
						out.println(Duration.between(startTime, timestamp).toMillis()+"\t"+msg);
						Thread.sleep(5000);
						
					} catch (Exception e) {
						log.severe("Error when polling order book image: "+e.getMessage());
						e.printStackTrace();
					}
				}
			}
		};
		t2.start();
			
	}

}
