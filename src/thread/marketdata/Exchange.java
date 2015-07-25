package thread.marketdata;

public enum Exchange {
	SANDBOX ("https://public.sandbox.exchange.coinbase.com",
			"https://api-public.sandbox.exchange.coinbase.com/products",
			"https://api-public.sandbox.exchange.coinbase.com/products/BTC-USD/book?level=2",
			"https://api-public.sandbox.exchange.coinbase.com/products/BTC-USD/book?level=3",
			"wss://ws-feed-public.sandbox.exchange.coinbase.com"), 
	PRODUCTION ("https://exchange.coinbase.com",
			"https://api.exchange.coinbase.com/products",
			"https://api.exchange.coinbase.com/products/BTC-USD/book?level=2",
			"https://api.exchange.coinbase.com/products/BTC-USD/book?level=3",
			"wss://ws-feed.exchange.coinbase.com"),
	REPLAY ("",
			"/Users/nick/Dev/Data/Coinbase-products.txt",
			"/Users/nick/Dev/Data/Coinbase-L2-webservice-images.txt",
			"/Users/nick/Dev/Data/Coinbase-L3-webservice-images.txt",
			"/Users/nick/Dev/Data/Coinbase-websocket-updates.txt");
	
	public final String website;
	public final String products;
	public final String level2orderbook;
	public final String level3orderbook;
	public final String websocket;
	
	Exchange (String website, String products, String level2orderbook, String level3orderbook, String websocket) {
		this.website = website;
		this.products = products;
		this.level2orderbook = level2orderbook;
		this.level3orderbook = level3orderbook;
		this.websocket = websocket;
	}
}
