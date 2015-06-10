package va.policy;

import java.io.*;
import java.util.*;

public class Inforce {
	protected List<Policy> vPolicy;
	protected Set<Integer> setFundNum;
	protected Map<Integer, Double> mapFundFee;	
	protected Set<String> setProductType;
	
	public Inforce() {
		vPolicy = new ArrayList<Policy>();
		setFundNum = new HashSet<Integer>();
		mapFundFee = new HashMap<Integer, Double>();
		setProductType = new HashSet<String>();
	}

	public int size() {
		return vPolicy.size();
	}
	
	public Policy get(int i) {
		return vPolicy.get(i);
	}
	
	public void addPolicy(Policy p) {
		vPolicy.add(p);
		for(int k=0; k<p.vFundNum.length; ++k) {
			if(p.vFundNum[k]>0) {
				setFundNum.add(p.vFundNum[k]);
				if(!mapFundFee.containsKey(p.vFundNum[k])) {
					mapFundFee.put(p.vFundNum[k], p.vFundFee[k]);
				}
			}
		}
		setProductType.add(p.productType);
	}
	
	public void saveToCSV(String filename) throws Exception {
		if(vPolicy.isEmpty()) {
			return;
		}
		
		String newline = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append(vPolicy.get(0).getHeader());
		for(int i=0; i<vPolicy.size(); ++i) {
			sb.append(newline);
			sb.append(vPolicy.get(i).getString());
		}
				
		FileWriter outFile = new FileWriter(filename);
		PrintWriter out = new PrintWriter(outFile);
		out.print(sb.toString());
		out.close();
	}
	
	public List<Integer> getFundNum() {
		List<Integer> listFundNum = new ArrayList<Integer>();
		for(Integer k : setFundNum) {
			listFundNum.add(k);
		}
		Collections.sort(listFundNum);
		
		return listFundNum;
	}
	
	public List<String> getProductType() {
		List<String> listProductType = new ArrayList<String>();
		for(String s :setProductType) {
			listProductType.add(s);
		}
		
		return listProductType;
	}
	
	public Map<Integer, Double> getFundFee() {
		return mapFundFee;
	}
	
}
