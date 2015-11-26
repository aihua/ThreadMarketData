package main;

public class RawOrderBookUpdate {
	String type;
	String time;
	String product_id;
	public String sequence;
	String order_id;
	String side;
	String order_type;
	
	// Limit orders only
	String size;
	String price;

	// Market orders only
	String funds;
	
	// Open and Done messages only
	String remaining_size;
	
	// Done messages only
	String reason;
	
	// Match messages only
	String maker_order_id;
	String taker_order_id;
	
	// Change messages only
	String new_size;
	String old_size;
	String new_funds;
	String old_funds;
	
	// Error messages only
	String message;
	
	RawOrderBookUpdate(String type, String time, String product_id, String sequence, String order_id, String side,
			String order_type, String size, String price, String funds, String remaining_size, String reason,
			String maker_order_id, String taker_order_id, String new_size, String old_size, String new_funds,
			String old_funds, String message) {
		this.type = type;
		this.time = time;
		this.product_id = product_id;
		this.sequence = sequence;
		this.order_id = order_id;
		this.side = side;
		this.order_type = order_type;
		this.size = size;
		this.price = price;
		this.funds = funds;
		this.remaining_size = remaining_size;
		this.reason = reason;
		this.maker_order_id = maker_order_id;
		this.taker_order_id = taker_order_id;
		this.new_size = new_size;
		this.old_size = old_size;
		this.new_funds = new_funds;
		this.old_funds = old_funds;
		this.message = message;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		if (type != null) {sb.append("type "+type+", ");}
		if (time != null) {sb.append("time "+time.toString()+", ");}
		if (product_id != null) {sb.append("product_id "+product_id+", ");}
		if (sequence != null) {sb.append("sequence "+sequence+", ");}
		if (order_id != null) {sb.append("order_id "+order_id+", ");}
		if (side != null) {sb.append("side "+side+", ");}
		if (order_type != null) {sb.append("order_type "+order_type+", ");}
		if (size != null) {sb.append("size "+size+", ");}
		if (price != null) {sb.append("price "+price+", ");}
		if (funds != null) {sb.append("funds "+funds);}
		if (remaining_size != null) {sb.append("remaining_size "+remaining_size+", ");}
		if (reason != null) {sb.append("reason "+reason+", ");}
		if (maker_order_id != null) {sb.append("maker_order_id "+maker_order_id+", ");}
		if (taker_order_id != null) {sb.append("taker_order_id "+taker_order_id+", ");}
		if (new_size != null) {sb.append("new_size "+new_size+", ");}
		if (old_size != null) {sb.append("old_size "+old_size+", ");}
		if (new_funds != null) {sb.append("new_funds "+new_funds+", ");}
		if (old_funds != null) {sb.append("old_funds "+old_funds+", ");}
		if (message != null) {sb.append("message "+message+", ");}
		sb.append("]");
		return sb.toString();
	}
}
