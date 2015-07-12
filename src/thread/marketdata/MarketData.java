package thread.marketdata;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.google.gson.Gson;

public class MarketData {

	final static Logger log = Logger.getLogger("thread.marketdata.MarketData");
	
	public static void main(String[] args) {
		// Get products:
		Products p = new Products(Exchange.SANDBOX);
		log.info("Got products from exchange: "+p.getProducts().toString());
		
		/* Get order book image:
		final Gson gson = new Gson();
		try {
			URL url = new URL(Exchange.SANDBOX.API + "/products/BTC-USD/book?level=2");
		    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			RawOrderBookImage o = gson.fromJson(in.readLine(), RawOrderBookImage.class);
			System.out.println(o.toString());
		} catch (Exception e) {
			log.severe("Error: "+e.getMessage());
			e.printStackTrace();
		} */
	
		WebSocketClient client = new WebSocketClient(new SslContextFactory());
		MarketDataSocket socket = new MarketDataSocket(p.getProducts().get(0));
		try {
			client.start();
			URI uri = new URI(Exchange.PRODUCTION.websocket);
			log.info("Connecting to:"+uri.toString());
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			client.connect(socket, uri, request);
			socket.awaitClose(5, TimeUnit.SECONDS);
			
		} catch (Exception e) {
			log.severe("Caught exception opening websocket: "+e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				log.info("Closing websocket client");
				client.stop();
			} catch (Exception e) {
				log.severe("Caught exception closing client: "+e.getMessage());
				e.printStackTrace();
			}
		}

	}

}
