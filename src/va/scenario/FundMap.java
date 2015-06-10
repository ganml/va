package va.scenario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class FundMap {
	protected String fundMapFile;
	protected Map<Integer, double[]> fundMap;
	protected int numIndex;
	
	public FundMap(String fundMapFile) throws Exception {
		fundMap = new HashMap<Integer, double[]>();
		this.fundMapFile = fundMapFile;
		
		loadFundMap();
	}
	
	public int getNumIndex() {
		return numIndex;
	}

	protected void loadFundMap() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(fundMapFile));
		String line;
		String[] cell;
		String header = br.readLine();
		cell = header.split(",");
		if(cell.length < 2) {
			br.close();
			throw new Exception("bad fund map header line: " + header);
		}
		int numCol = cell.length;
		numIndex = numCol - 1;
		while(true) {
			line = br.readLine();
			if(line == null || line.trim().equals("")) {
				break;
			}
			cell = line.split(",");
			if(cell.length != numCol) {
				br.close();
				throw new Exception("bad fund map line: " + line);
			}
			double[] vw = new double[cell.length-1];
			double dSum = 0.0;
			for(int j=1; j<cell.length; ++j) {
				vw[j-1] = Double.parseDouble(cell[j]);
				dSum += vw[j-1];
			}
			if(Math.abs(dSum-1) > 1e-4) {
				br.close();
				throw new Exception("sum of fund weights != 1: " + line);
			}
			
			fundMap.put(Integer.parseInt(cell[0]), vw);
		}
		br.close();
	}
	
	public double[] get(int fundNum) {
		return fundMap.get(fundNum);
	}
	
	public List<Integer> getFundNumber() {
		List<Integer> res = new ArrayList<Integer>();
		for(int k: fundMap.keySet()) {
			res.add(k);
		}
		
		return res;
	}
	
	public Map<Integer, Double> convert(double[] vIndexShock) throws Exception {
		if(vIndexShock.length != numIndex) {
			throw new Exception("vIndexShock does not match fundmap numIndex");
		}
		
		Map<Integer, Double> res = new HashMap<Integer, Double>();
		for(int k : fundMap.keySet()) {			
			double dSum = 0.0;
			for(int j=0; j<numIndex; ++j) {
				dSum += vIndexShock[j] * fundMap.get(k)[j];
			}
			res.put(k, dSum);
		}
			
		return res;
	}
}
