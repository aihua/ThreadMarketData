package thread.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.logging.Logger;

import com.google.gson.Gson;

import thread.marketdata.Exchange;
import thread.marketdata.Products;
import thread.marketdata.RawOrderBookUpdate;

public class DataReplaySocket implements Runnable {
	// Emulates a MarketDataSocket but plays back data from disk
	// NB this implements Runnable because the actual MarketDataSocket is called on demand when data received
	
	final static Logger log = Logger.getLogger("thread.marketdata.DataReplaySocket");

	Exchange exchange;
    String currencyPair;
    Queue<RawOrderBookUpdate> queue;
    Instant startTime;
    FileReader fr;
    BufferedReader br;
    String line;
    Gson gson;
	
	public DataReplaySocket(Exchange exchange, String currencyPair, Queue<RawOrderBookUpdate> queue) {
		log.info("Initializing DataReplaySocket");
		if (! exchange.equals(Exchange.REPLAY)) {
			log.severe("Trying to replay data from a non-REPLAY Exchange!");
		}
		gson = new Gson();
		this.exchange = exchange;
		this.currencyPair = currencyPair;
		this.queue = queue;
		try {
			fr = new FileReader(exchange.websocket);
		} catch (FileNotFoundException e) {
			log.severe("Can't open file for reading: "+e.getMessage());
			e.printStackTrace();
		}
		br = new BufferedReader(fr);
		startTime = Instant.now();
	}

	public void run() {
		try {
			while (((line = br.readLine()) != null) && ! Thread.interrupted()) {
				String[] array = line.split("\t");
				String timeString = array[0];
				String msg = array[1];
				
				Long currentElapsed = Duration.between(startTime, Instant.now()).toMillis();
				Long targetElapsed = Long.parseLong(timeString);
				if (currentElapsed < targetElapsed){
					Thread.sleep(targetElapsed - currentElapsed);
				}
				
				RawOrderBookUpdate r = gson.fromJson(msg, RawOrderBookUpdate.class);
				queue.add(r);
			}
			
			br.close();
			fr.close();
		} catch (Exception e1) {
			log.severe("Caught error reading from file: "+e1.getMessage());
			e1.printStackTrace();
		}

	}

	
}
