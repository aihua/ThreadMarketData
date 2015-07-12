package thread.marketdata;

import java.util.Arrays;

public class RawOrderBookImage {
	String sequence;
	String[][] bids; // [ [price, size, num-orders], [price, size, num-orders] ... ]
	String[][] asks;
	
	RawOrderBookImage(String sequence, String[][] bids, String[][] asks) {
		if (sequence != null && bids != null && asks != null) {
			this.sequence = sequence;
			this.bids = bids;
			this.asks = asks;
		} else {
			throw new NullPointerException("Tried to create a RawOrderBookImage with null value");
		}
	}
	
	@Override
	public String toString() {
		StringBuffer retval = new StringBuffer();
		retval.append("Sequence "+sequence+": Bids [");
		for (String[] stringarray: bids) {
			retval.append(Arrays.toString(stringarray));
		}
		retval.append("] Asks: [");
		for (String[] stringarray: asks) {
			retval.append(Arrays.toString(stringarray));
		}		
		retval.append("]");
		return retval.toString();
	}
	
}
