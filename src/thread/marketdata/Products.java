package thread.marketdata;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Products {
	
	// Assumes products available don't change during the life of the session
	
	final Logger log = Logger.getLogger("thread.marketdata.Products");
	final Gson gson = new Gson();
	final Type collectionType = new TypeToken<ArrayList<Product>>(){}.getType();
	
	Exchange exchange;
	ArrayList<Product> products;

	Products(Exchange exchange) {
		this.exchange = exchange;
		try {
			URL url = new URL(exchange.API + "/products");
		    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			this.products = gson.fromJson(in.readLine(), collectionType);
		} catch (Exception e) {
			log.severe("Error getting products from exchange: "+e.getMessage());
			e.printStackTrace();
		}
	}

	ArrayList<Product> getProducts() {
		return products;
	}
	
	class Product {
		String id;
		String base_currency;
		String quote_currency;
		Double base_min_size;
		Double base_max_size;
		Double quote_increment;
		String display_name;
		
		Product(String id, String base_currency, String quote_currency, 
				String base_min_size, String base_max_size, String quote_increment, String display_name) {
			this.id = id;
			this.base_currency = base_currency;
			this.quote_currency = quote_currency;
			this.base_max_size = Double.parseDouble(base_max_size);
			this.quote_increment = Double.parseDouble(quote_increment);
			this.display_name = display_name;
		}
		
		@Override
		public String toString() {
			return String.join(";", id, base_currency, quote_currency, base_max_size.toString(),
					quote_increment.toString(), display_name);
		}
	}
}
