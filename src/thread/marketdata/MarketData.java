package thread.marketdata;

import java.util.logging.Logger;

public class MarketData {

	final static Logger log = Logger.getLogger("thread.marketdata.MarketData");
	
	public static void main(String[] args) {
		// Get products:
		Products p = new Products(Exchange.PRODUCTION);
		log.info("Got products from exchange: "+p.getProducts().toString());
	
		Thread OrderBookBuilderThread = new Thread(
				new OrderBookBuilder(Exchange.PRODUCTION, p.products.get(0)),"OrderBookBuilderThread");
		OrderBookBuilderThread.start();
		

	}

}
