package main;

public class RealtimeEvent {

	RawOrderBookUpdate delta;
	Integer sequence;
	String product_id;
	String type;
	
	RealtimeEvent(RawOrderBookUpdate delta) {
		this.delta = delta;
		this.sequence = Integer.parseInt(delta.sequence);
		this.product_id = delta.product_id;
		this.type = delta.type;
	}
	
	
	
}
