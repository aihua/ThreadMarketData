package main;

public enum Exchange {
	
	// [0] Website
	// [1] REST API
	// [2] Level 2 Order Book
	// [3] Level 3 Order Book
	// [4] Websocket feed
	
	SANDBOX ("https://public.sandbox.exchange.coinbase.com",
			"https://api-public.sandbox.exchange.coinbase.com",
			"https://api-public.sandbox.exchange.coinbase.com/products/BTC-USD/book?level=2",
			"https://api-public.sandbox.exchange.coinbase.com/products/BTC-USD/book?level=3",
			"wss://ws-feed-public.sandbox.exchange.coinbase.com"), 
	PRODUCTION ("https://exchange.coinbase.com",
			"https://api.exchange.coinbase.com",
			"https://api.exchange.coinbase.com/products/BTC-USD/book?level=2",
			"https://api.exchange.coinbase.com/products/BTC-USD/book?level=3",
			"wss://ws-feed.exchange.coinbase.com"),
	REPLAY ("",
			"/Users/nick/Dev/Data/Coinbase-products.txt",
			"/Users/nick/Dev/Data/Coinbase-L2-webservice-images.txt",
			"/Users/nick/Dev/Data/Coinbase-L3-webservice-images.txt",
			"/Users/nick/Dev/Data/Coinbase-websocket-updates.txt");
	
	// TODO: Be consistent with single BTC-USD or multiple product feed
	
	public final String website;
	public final String rest;
	public final String level2orderbook;
	public final String level3orderbook;
	public final String websocket;
	
	Exchange (String website, String rest, String level2orderbook, String level3orderbook, String websocket) {
		this.website = website;
		this.rest = rest;
		this.level2orderbook = level2orderbook;
		this.level3orderbook = level3orderbook;
		this.websocket = websocket;
	}
}
