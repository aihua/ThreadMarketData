package main;

public class Order {
	enum OrderType {MARKET, LIMIT};
	
	OrderType type;
	String product_id; // Maybe this should be of type Products.Product...
	String order_id;
	String side;
	
	// Limit orders only
	Double size;
	Double price;
	
	Order(Double price, Double size, String order_id) {
		// Used for creation from initial REST order book image
		this.price = price;
		this.size = size;
		this.order_id = order_id;
	}
	
	Order(OrderType type, String product_id, String order_id, String side, Double size, Double price) {
		// Used for creation from web service delta
		this.type = type;
		this.product_id = product_id;
		this.order_id = order_id;
		this.side = side;
		this.size = size;
		this.price = price;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		if (type != null) {sb.append("type "+type+", ");}
		if (product_id != null) {sb.append("product_id "+product_id+", ");}
		if (order_id != null) {sb.append("order_id "+order_id+", ");}
		if (side != null) {sb.append("side "+side+", ");}
		if (size != null) {sb.append("size "+size+", ");}
		if (price != null) {sb.append("price "+price+", ");}
		sb.append("]");
		return sb.toString();
	}
}
