package main;

import java.time.Instant;

public class Order {
	enum OrderType {MARKET, LIMIT};
	
	OrderType type;
	Instant time;
	String product_id; // Maybe this should be of type Products.Product...
	Integer sequence;
	String order_id;
	String side;
	
	// Limit orders only
	Double size;
	Double price;

	// Market orders only
	String funds;
	
	Order(Double price, Double size, String order_id) {
		// Used for creation from initial REST order book image
		this.price = price;
		this.size = size;
		this.order_id = order_id;
	}
	
	Order(OrderType type, Instant time, String product_id, Integer sequence, String order_id, String side,
			Double size, Double price, String funds) {
		// (possibly) used for creation from web service delta
		this.type = type;
		this.time = time;
		this.product_id = product_id;
		this.sequence = sequence;
		this.order_id = order_id;
		this.side = side;
		this.size = size;
		this.price = price;
		this.funds = funds;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		if (type != null) {sb.append("type "+type+", ");}
		if (time != null) {sb.append("time "+time.toString()+", ");}
		if (product_id != null) {sb.append("product_id "+product_id+", ");}
		if (sequence != null) {sb.append("sequence "+sequence+", ");}
		if (order_id != null) {sb.append("order_id "+order_id+", ");}
		if (side != null) {sb.append("side "+side+", ");}
		if (size != null) {sb.append("size "+size+", ");}
		if (price != null) {sb.append("price "+price+", ");}
		if (funds != null) {sb.append("funds "+funds);}
		sb.append("]");
		return sb.toString();
	}
}
