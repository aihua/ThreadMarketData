package main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class MarketData {

	final static Logger log = Logger.getLogger("main.MarketData");
	static Integer gatewayPort;
	static String builderType;
	static String currencyPair;
	static Exchange exchange;
	
	public static void main(String[] args) {
		log.info("Starting up Market Data publisher...");
		log.finest("Finest logging enabled");
		// Load properties
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("thread.properties"));
		} catch (FileNotFoundException e) {
			log.severe("Cannot find properties file: "+e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.severe("Caught IOException trying to load properties: "+e.getMessage());
			e.printStackTrace();
		}
		gatewayPort = Integer.parseInt(props.getProperty("gatewayPort"));
		builderType = props.getProperty("builderType");
		currencyPair = props.getProperty("currencyPair");
		exchange = Exchange.valueOf(props.getProperty("exchange"));
		log.info("Got properties: "+props.toString());
		
		// Get products:
		Products p = new Products(exchange);
		log.info("Got products from exchange: "+p.getProducts().toString());
		//TODO: Check that currencyPair specified in props is available on exchange
	
		LinkedBlockingQueue<OrderBook> queue = new LinkedBlockingQueue<OrderBook>();
		Thread OrderBookBuilderThread;
		
		switch (builderType) {
		//TODO: Clarify whether REPLAY exchange influences this
		case "realtime":
			log.info("Starting realtime orderbook builder");
			OrderBookBuilderThread = new Thread(
					new RealtimeOrderBookBuilder(exchange, currencyPair, queue)
					,"OrderBookBuilderThread");
			OrderBookBuilderThread.start();
			break;
		case "periodic":
			log.info("Starting periodic orderbook builder");
			OrderBookBuilderThread = new Thread(
					new PeriodicOrderBookBuilder(exchange, currencyPair, queue),"OrderBookBuilderThread");
			OrderBookBuilderThread.start();
		default:
			log.severe("No valid orderbook builder type defined");
		}
		
		Thread ZeroMQPublisherThread = new Thread(
				new ZeroMQPublisher(gatewayPort, queue), "ZeroMQPublisherThread");
		ZeroMQPublisherThread.start();

	}

}
