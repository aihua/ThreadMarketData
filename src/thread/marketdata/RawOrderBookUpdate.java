package thread.marketdata;

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
	
}
