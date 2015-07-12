package thread.marketdata;

public enum Exchange {
	SANDBOX ("https://public.sandbox.exchange.coinbase.com",
			"https://api-public.sandbox.exchange.coinbase.com",
			"wss://ws-feed-public.sandbox.exchange.coinbase.com"), 
	PRODUCTION ("https://exchange.coinbase.com",
			"https://api.exchange.coinbase.com",
			"wss://ws-feed.exchange.coinbase.com");
	
	final String website;
	final String API;
	final String websocket;
	
	Exchange (String website, String API, String websocket) {
		this.website = website;
		this.API = API;
		this.websocket = websocket;
	}
}
