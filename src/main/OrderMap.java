package main;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

public class OrderMap {

	final Logger log = Logger.getLogger("main.OrderMap");
	private TreeMap<Double, TreeMap<String, Order>> bidMap; // price -> (order_id -> order)
	private TreeMap<Double, TreeMap<String, Order>> askMap;
	private Integer sequenceNumber;
	private Instant MarketTime;
	RawOrderBookImage initialImage;
	String currencyPair;

	OrderMap(String currencyPair, RawOrderBookImage initialImage, Integer sequenceNumber, Instant MarketTime) {
		// Initialize variables
		this.currencyPair = currencyPair;
		this.initialImage = initialImage;
		this.MarketTime = MarketTime;
		bidMap = new TreeMap<Double, TreeMap<String, Order>>();
		askMap = new TreeMap<Double, TreeMap<String, Order>>();
		// Populate tree maps with image data
		for (String[] array: initialImage.bids) {
			// [price, size, order_id], [price, size, order_id]...
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			String order_id = array[2];
			addToMap("bid", new Order(price, size, order_id));
		}
		for (String[] array: initialImage.asks) {
			Double price = Double.parseDouble(array[0]);
			Double size = Double.parseDouble(array[1]);
			String order_id = array[2];
			addToMap("ask", new Order(price, size, order_id));
		}
		setSequence(sequenceNumber);
	}
	
	void addToMap(String side, Order order) {
		TreeMap<Double, TreeMap<String, Order>> map = null;
		switch (side) {
			case "bid": map = bidMap;
				break;
			case "ask": map = askMap;
				break;
			default: log.severe("Called addToMap with invalid side: side "+side+", order "+order.order_id);
				dumpOrderMap();
				break;
		}
			
		if (map.containsKey(order.price)) {
			TreeMap<String, Order> ordersAtLevel = map.get(order.price);
			if (! ordersAtLevel.containsKey(order.order_id)) {
				ordersAtLevel.put(order.order_id, order);
				log.finest("Added order "+order.toString()+" to existing order "+side+" map at that price");
			} else {
				log.severe("Attempted to add order "+order.toString()+" to "+side+" map when it already belongs and failed!");
				dumpOrderMap();
			}
		} else {
			TreeMap<String, Order> ordersAtLevel = new TreeMap<String, Order>();
			ordersAtLevel.put(order.order_id, order);
			map.put(order.price, ordersAtLevel);
			log.finest("Added order "+order.toString()+" to new order map at that price");
		}
	}
	
	void changeInMap(Double price, String order_id, Double newSize, Double newFunds) {
		if (bidMap.containsKey(price)) { // I think this protects me from a NPE
			// Bit weird I don't have to check side?
			// It's on the book; it must be a resting limit order
			// Relies on the fact that a price can EITHER be on the bid or ask, but not both
			TreeMap<String, Order> ordersAtLevel = bidMap.get(price);
			if (ordersAtLevel.containsKey(order_id)) {
				// It's on the book; it must be a resting limit order
				Order order = ordersAtLevel.get(order_id);
				order.size = newSize;
				// Ignore funds
			} else {
				log.severe("Tried to changeInMap "+order_id+": price in bidMap, but order not in level TreeMap");
				dumpOrderMap();
			}
			
		} else if (askMap.containsKey(price)) {
			TreeMap<String, Order> ordersAtLevel = askMap.get(price);
			if (ordersAtLevel.containsKey(order_id)) {
				Order order = ordersAtLevel.get(order_id);
				order.size = newSize;
				// Ignore funds
			} else {
				log.severe("Tried to changeInMap "+order_id+": price in askMap, but order not in level TreeMap");
				dumpOrderMap();
			}
		} else {
			log.severe("Tried to changeInMap "+order_id+" but price "+price+" in neither bid nor ask maps");
			dumpOrderMap();
		}
	}
	
	
	void removeFromMap(String side, Double price, String order_id) {
		TreeMap<Double, TreeMap<String, Order>> map = null;
		switch (side) {
			case "bid": map = bidMap;
				break;
			case "ask": map = askMap;
				break;
			default: log.severe("Called removeFromMap with invalid side: side "+side+", price "+price+", order "+order_id);
				dumpOrderMap();
				break;
		}
		
		if (map.containsKey(price)) {
			TreeMap<String, Order> ordersAtLevel = map.get(price);
			ordersAtLevel.remove(order_id);
			if (ordersAtLevel.isEmpty()) {
				map.remove(price); // for efficiency reasons
			}
		} else {
			log.severe("Attempted to remove order "+order_id+" from map when its price was not present!");
			dumpOrderMap();
		}
	}
	
	Integer getSequence() {
		return sequenceNumber;
	}
	
	void setSequence (Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	
	Instant getMarketTime() {
		return MarketTime;
	}
	
	void setMarketTime(Instant MarketTime) {
		this.MarketTime = MarketTime;
	}
	
	public OrderBook emitOrderBook() {
		// Sync (private) tree maps to new immutable order book object and emit
		Instant OrderBookBuilderStartTime = Instant.now();
		Iterator<Double> priceIterator;
		priceIterator = bidMap.descendingKeySet().iterator();
		// Oh lord this is ugly.
		TreeMap<String, Order> ordersAtLevel;
		Iterator<String> orderIterator;
		// Bid0
		Double BidPrice0 = priceIterator.next();
		Double BidSize0 = 0.0;
		ordersAtLevel = bidMap.get(BidPrice0);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			BidSize0 += ord.size;
		}
		// Bid1
		Double BidPrice1 = priceIterator.next();
		Double BidSize1 = 0.0;
		ordersAtLevel = bidMap.get(BidPrice1);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			BidSize1 += ord.size;
		}
		// Bid2
		Double BidPrice2 = priceIterator.next();
		Double BidSize2 = 0.0;
		ordersAtLevel = bidMap.get(BidPrice2);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			BidSize2 += ord.size;
		}
		priceIterator = askMap.keySet().iterator();
		// Ask0
		Double AskPrice0 = priceIterator.next();
		Double AskSize0 = 0.0;
		ordersAtLevel = askMap.get(AskPrice0);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			AskSize0 += ord.size;
		}
		// Ask1
		Double AskPrice1 = priceIterator.next();
		Double AskSize1 = 0.0;
		ordersAtLevel = askMap.get(AskPrice1);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			AskSize1 += ord.size;
		}
		// Ask2
		Double AskPrice2 = priceIterator.next();
		Double AskSize2 = 0.0;
		ordersAtLevel = askMap.get(AskPrice2);
		orderIterator = ordersAtLevel.keySet().iterator();
		while (orderIterator.hasNext()) {
			String orderID = orderIterator.next();
			Order ord = ordersAtLevel.get(orderID);
			AskSize2 += ord.size;
		}
		Instant OrderBookBuilderEndTime = Instant.now();
		OrderBook ob = new OrderBook(currencyPair, MarketTime, OrderBookBuilderStartTime, 
				OrderBookBuilderEndTime, sequenceNumber, BidPrice2, BidSize2, 
				BidPrice1, BidSize1, BidPrice0, BidSize0, 
				AskPrice0, AskSize0, AskPrice1, AskSize1, 
				AskPrice2, AskSize2); 
		
		return ob;
	}
	
	void dumpOrderMap() {
		StringBuffer sb = new StringBuffer();
		try {
			Iterator<Double> priceIterator;
			sb.append("BIDS:");
			priceIterator = bidMap.descendingKeySet().iterator();
			while (priceIterator.hasNext()) {
				Double price = priceIterator.next();
				sb.append("\nPrice "+price+": ");
				TreeMap<String, Order> ordersAtLevel = bidMap.get(price);
				Iterator<String> orderIterator = ordersAtLevel.keySet().iterator();
				while (orderIterator.hasNext()) {
					String orderID = orderIterator.next();
					Order ord = ordersAtLevel.get(orderID);
					sb.append(ord.toString());
				}
			}
			sb.append("\nASKS:");
			priceIterator = askMap.keySet().iterator();
			while (priceIterator.hasNext()) {
				Double price = priceIterator.next();
				sb.append("\nPrice "+price+": ");
				TreeMap<String, Order> ordersAtLevel = bidMap.get(price);
				Iterator<String> orderIterator = ordersAtLevel.keySet().iterator();
				while (orderIterator.hasNext()) {
					String orderID = orderIterator.next();
					Order ord = ordersAtLevel.get(orderID);
					sb.append(ord.toString());
				}
			}
			String filename = "/Users/nick/Dev/Local/ThreadMarketData/OrderMapDump.txt";
			FileWriter fw = new FileWriter(filename);
			PrintWriter out = new PrintWriter(fw);
			out.println(sb.toString());
			out.close();
			log.severe("Dumped orderMap to "+filename);
		} catch (Exception e) {
			log.severe("Caught exception trying to dumpOrderMap: "+e.getMessage());
			try {
				String filename = "/Users/nick/Dev/Local/ThreadMarketData/OrderMapDump.txt";
				FileWriter fw = new FileWriter(filename);
				PrintWriter out = new PrintWriter(fw);
				out.println(sb.toString());
				out.close();
				log.severe("Dumped orderMap to "+filename);
			} catch (Exception e2) {};
		}
	}
}
