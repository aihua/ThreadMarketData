package thread.marketdata;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket(maxTextMessageSize = 1024 * 1024)
public class MarketDataSocket {
	 
	private final CountDownLatch closeLatch;
	final static Logger log = Logger.getLogger("thread.marketdata.MarketDataSocket");
 
    Session session;
 
    public MarketDataSocket() {
    	this.closeLatch = new CountDownLatch(1);
        log.info("Created MarketDataSocket");
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
        try {
        	Future<Void> fut;
        	fut = session.getRemote().sendStringByFuture("{\"type\": \"subscribe\", \"product_id\": \"BTC-USD\"}");
        	fut.get(60, TimeUnit.SECONDS);
 //       	session.close(StatusCode.NORMAL, "I'm done");
        } catch (Exception e) {
        	log.severe("Caught exception onConnect: "+e.getMessage());
        	e.printStackTrace();
        }
    }
 
    @OnWebSocketMessage
    public void onMessage(String msg) {
        log.info("Websocket onMessage: "+msg);
    }
    
    public void sendMessage(String msg) {
    	Future<Void> fut;
        fut = session.getRemote().sendStringByFuture(msg);
        try {
        	fut.get();
        } catch (Exception e) {
        	log.severe("Caught exception from websocket sendMessage: "+e.getMessage());
        	e.printStackTrace();
        }
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

