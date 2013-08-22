package glt;

import java.util.HashMap;
import java.util.Map;

public enum Strand {
	
	Positive("+"),
	Negative("-");
	
	private final String symbol;
	
	// letter -> Base
	static final private Map<String, Strand> all = new HashMap<String, Strand>();

	Strand(String symbol) {
		this.symbol = symbol;
	}
	
	public String getSymbol() {
		return this.symbol;
	}
	
	static public Strand forSymbol(String symbol) {
		
		initMapping();
		
		return all.get(symbol);
	}
	
	private static Map<String, Strand> initMapping() {
        if (all.size() == 0) {
	        for (Strand strand : values()) {
	            all.put(strand.symbol, strand);
	        }
        }
        
        return all;
    }
}
