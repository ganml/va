package va.montecarlo;

import java.util.*;

public class Result {
	public int recordID;
	public String irShock;
	public String eqShock;
	public int scenario;
	
	public double riderFee;
	public double fmv; // gmdb+gmwb+gmmb-riskCharge
	public double riskCharge;
	public double gmdb; // pv of death guarantee payment
	public double gmwb; // pv of withdrawal guarantee payment
	public double gmmb; // pv of maturity guarantee payment
	public double[] av;
	protected List<String> listKeys;
	
	protected Map<String, Object> mapOther;
	
	public Result() {
		mapOther = new HashMap<String, Object>();
		listKeys = new ArrayList<String>();
	}
	
	public void addResult(String name, Object value) {
		if(mapOther.containsKey(name)) {
			
		} else {
			listKeys.add(name);
			mapOther.put(name, value);
		}
	}
	
	public Object getResult(String name) throws Exception {
		if(mapOther.containsKey(name)) {
			return mapOther.get(name);
		} else {
			throw new Exception("Cannot find result for " + name);
		}
	}
}
