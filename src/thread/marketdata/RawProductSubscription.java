package thread.marketdata;

public class RawProductSubscription {
	String type;
	String product_id;
	
	public RawProductSubscription(String currencyPair) {
		this.type = "subscribe";
		this.product_id = currencyPair;
	}
}
