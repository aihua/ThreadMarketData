package thread.marketdata;

import java.time.Instant;

public class OrderBook {
		
	String currencyPair;
	Instant MarketTime;
	Instant OrderBookBuilderStartTime;
	Instant OrderBookBuilderEndTime;
	Integer sequenceNumber;
	
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
	
	public OrderBook(String currencyPair, Instant MarketTime, Instant OrderBookBuilderStartTime, 
			Instant OrderBookBuilderEndTime, Integer sequenceNumber, Double BidPrice2, Double BidSize2, 
			Double BidPrice1, Double BidSize1, Double BidPrice0, Double BidSize0, 
			Double AskPrice0, Double AskSize0, Double AskPrice1, Double AskSize1, 
			Double AskPrice2, Double AskSize2) {
		this.currencyPair = currencyPair;
		this.MarketTime = MarketTime;
		this.OrderBookBuilderStartTime = OrderBookBuilderStartTime;
		this.sequenceNumber = sequenceNumber;
		this.BidPrice2 = BidPrice2;
		this.BidSize2 = BidSize2;
		this.BidPrice1 = BidPrice1;
		this.BidSize1 = BidSize1;
		this.BidPrice0 = BidPrice0;
		this.BidSize0 = BidSize0;
		this.AskPrice0 = AskPrice0;
		this.AskSize0 = AskSize0;
		this.AskPrice1 = AskPrice1;
		this.AskSize1 = AskSize1;
		this.AskPrice2 = AskPrice2;
		this.AskSize2 = AskSize2;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("OrderBook: currencyPair "+currencyPair);
		sb.append(" MarketTime "+MarketTime.toString());
		sb.append(" OrderBookBuilderStartTime "+OrderBookBuilderStartTime.toString());
		sb.append(" OrderBookBuilderEndTime "+OrderBookBuilderEndTime.toString());
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
