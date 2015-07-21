package thread.marketdata;

public class RawProductSubscription {
	String type;
	String product_id;
	
	public RawProductSubscription(Products.Product p) {
		this.type = "subscribe";
		this.product_id = p.id;
	}
}
