package thread.marketdata;

import thread.marketdata.Products.Product;

public class OrderBook {
		
	Product product;
	Integer timeStampMillis;
	Integer sequenceNumber;
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
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("OrderBook: Product "+product.id);
		sb.append(" isStale "+isStale.toString());
		sb.append(" sequence number "+sequenceNumber.toString());
		sb.append(" [Bid2 "+BidPrice2+"x"+BidSize2);
		sb.append(" Bid1 "+BidPrice1+"x"+BidSize1);
		sb.append(" Bid0 "+BidPrice0+"x"+BidSize0);
		sb.append(" Ask0 "+AskPrice0+"x"+AskSize0);
		sb.append(" Ask1 "+AskPrice1+"x"+AskSize1);
		sb.append(" Ask2 "+AskPrice2+"x"+AskSize2+"]");
		return sb.toString();
	}
}
