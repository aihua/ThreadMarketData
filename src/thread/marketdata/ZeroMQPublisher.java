package thread.marketdata;

import java.util.concurrent.LinkedBlockingQueue;
import thread.common.*;

import java.util.logging.Logger;

import org.zeromq.ZMQ;

import com.google.gson.Gson;

public class ZeroMQPublisher implements Runnable {

	final Logger log = Logger.getLogger("thread.marketdata.ZeroMQPublisher");
	final Gson gson = new Gson();
	
	LinkedBlockingQueue<OrderBook> inboundQueue;
	Integer gatewayPort;
	ZMQ.Context context;
	ZMQ.Socket socket;
	
	ZeroMQPublisher(Integer gatewayPort, LinkedBlockingQueue<OrderBook> inboundQueue) {
		log.info("Opening outbound ZeroMQ publisher on port 5555");
		this.inboundQueue = inboundQueue;
		this.gatewayPort = gatewayPort;
		context = ZMQ.context(1);
		socket = context.socket(ZMQ.PUB);
		socket.bind("tcp://*:"+gatewayPort.toString());
	}
	
	public void run() {
		while (! Thread.currentThread().isInterrupted()) {
			try {
				OrderBook ob = inboundQueue.take();
				String message = gson.toJson(ob);
				socket.send(message, 0);	
			} catch (InterruptedException e) {
				log.severe("ZeroMQPublisher interrupted waiting for next order book in queue: "+e.getMessage());
				e.printStackTrace();
			}
		}
		socket.close();
		context.term();
	}
}
