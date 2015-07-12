package thread.marketdata;

public class ProductSubscription {
	String type;
	String product_id;
	
	ProductSubscription(Products.Product p) {
		this.type = "subscribe";
		this.product_id = p.id;
	}
}
