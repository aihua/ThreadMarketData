package thread.marketdata;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MarketData {

	final static Logger log = Logger.getLogger("thread.marketdata.MarketData");
	
	public static void main(String[] args) {
		// Get products:
		Products p = new Products(Exchange.SANDBOX);
		log.info("Got products from exchange: "+p.getProducts().toString());
	
	OrderBookBuilder o = new OrderBookBuilder(Exchange.PRODUCTION, p.products.get(0));
	try {
		Thread.sleep(3000);
		o.shutdown();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}


	}

}
