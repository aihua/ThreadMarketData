package thread.marketdata;

import java.util.logging.Logger;

import thread.marketdata.Products.Product;

public class OrderBook {
	
	final Logger log = Logger.getLogger("thread.marketdata.OrderBookBuilder");
	
	Product product;
	Integer timeStampMillis;
	Boolean isStale;
	
	Double BidPrice2;
	Double BidSize2;
	Double BidPrice1;
	Double BidSize1;
	Double BidPrice0;
	Double BidSize0;
	
	Double AskPrice0;
	Double AskSize0;
	Double AskPrice1;
	Double AskSize1;
	Double AskPrice2;
	Double AskSize2;
	
	OrderBook(Product product) {
		this.product = product;
		isStale = true;
	}
}
