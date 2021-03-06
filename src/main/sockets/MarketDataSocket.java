package main.sockets;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import main.RawOrderBookUpdate;
import main.RawProductSubscription;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.google.gson.Gson;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class MarketDataSocket {
	 
	private final CountDownLatch closeLatch;
	final Gson gson = new Gson();
	final Logger log = Logger.getLogger("thread.marketdata.MarketDataSocket");
 
    Session session;
    String currencyPair;
    Queue<RawOrderBookUpdate> queue;
 
    public MarketDataSocket(String currencyPair, Queue<RawOrderBookUpdate> queue) {
    	this.closeLatch = new CountDownLatch(1);
    	this.currencyPair = currencyPair;
    	this.queue = queue;
        log.info("Created MarketDataSocket for currencyPair: "+currencyPair);
    }
  
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
    	return this.closeLatch.await(duration, unit);
    }
    
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log.info("Websocket onClose: "+Integer.toString(statusCode)+ " " + reason);
        this.session = null;
        this.closeLatch.countDown();
    }
 
    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        log.info("Websocket onConnect");
        RawProductSubscription ps = new RawProductSubscription(currencyPair);
        String psString = gson.toJson(ps);
        log.info("About to submit product subscription: "+psString);
        try {
        	@SuppressWarnings("unused")
			Future<Void> fut;
        	fut = session.getRemote().sendStringByFuture(psString);
        } catch (Exception e) {
        	log.severe("Caught exception onConnect: "+e.getMessage());
        	e.printStackTrace();
        }
    }
 
    @OnWebSocketMessage
    public void onMessage(String msg) {
            RawOrderBookUpdate r = gson.fromJson(msg, RawOrderBookUpdate.class);
            log.finest("Websocket onMessage: tick sequence "+r.sequence);
            queue.add(r);
    }
    
    public void logSessionStatus() {
    	if (session != null) {
    		log.info("Session idle timeout: "+session.getIdleTimeout());
        	log.info("Session WebSocketPolicy: "+session.getPolicy().toString());
        	log.info("Session ProtocolVersion: "+session.getProtocolVersion());
        	log.info("Session RemoteEndpoint: "+session.getRemote().toString());
        	log.info("Session RemoteAddress: "+session.getRemoteAddress().toString());
        	log.info("Session UpgradeRequest: "+session.getUpgradeRequest().toString());
        	log.info("Session UpgradeResponse: "+session.getUpgradeResponse().toString());
        	log.info("Session isOpen: "+session.isOpen());
        	log.info("Session isSecure: "+session.isSecure());
    	} else {
    		log.info("Session is null");
    	}
    }
}

