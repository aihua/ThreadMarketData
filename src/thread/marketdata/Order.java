package thread.marketdata;

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
		// Used for creation from webservice delta
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
}
