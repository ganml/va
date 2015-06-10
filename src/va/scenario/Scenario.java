package va.scenario;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

public class Scenario {
	protected Logger log = Logger.getLogger(Scenario.class);
	protected Map<Integer, double[][]> mapIndexScenario;
	protected Map<Integer, double[][]> mapFundScenario;
	protected String indexScenarioFolder;
	protected String fundMapFile;
	protected Map<Integer, String> mapIndexScenarioFile;
	protected List<Integer> listIndexNumber;
	protected List<Integer> listFundNumber;
	protected int numScenario;
	protected int numTimeStep;
	protected FundMap fundMap;
	
	public Scenario(String indexScenarioFolder, FundMap fundMap, int numScenario, int numTimeStep) throws Exception {
		this.indexScenarioFolder = indexScenarioFolder;
		this.numScenario = numScenario;
		this.numTimeStep = numTimeStep;
		this.fundMap = fundMap;
		
		readScenario();
		blendScenario();
	}
	
	public void readScenario() throws Exception {
		mapIndexScenarioFile = new HashMap<Integer, String>();
		listIndexNumber = new ArrayList<Integer>();
		
		// get index scenario file names
		File folder = new File(indexScenarioFolder);
		File indexMap = new File(folder, "indexMap.csv");
		BufferedReader br = new BufferedReader(new FileReader(indexMap));
		String line;
		String[] cell;
		line = br.readLine(); // header
		while(true) {
			line = br.readLine();
			if(line == null || line.trim().equals("")) {
				break;
			}
			cell = line.split(",");
			if(cell.length != 2) {
				br.close();
				throw new Exception("bad indexMap line: " + line);
			}
			
			int indexNum = Integer.parseInt(cell[0]);
			mapIndexScenarioFile.put(indexNum, cell[1].trim());
			listIndexNumber.add(indexNum);
		}
		br.close();
		
		// read scenario files
		mapIndexScenario = new HashMap<Integer, double[][]>();
		for(int i=0; i<listIndexNumber.size(); ++i) {
			double[][] sce = new double[numScenario][numTimeStep];
			
			File sceFile = new File(folder, mapIndexScenarioFile.get(listIndexNumber.get(i)));
			br = new BufferedReader(new FileReader(sceFile));
			for(int k=0; k<numScenario; ++k) {
				line = br.readLine();
				if(line == null || line.trim().equals("")) {
					br.close();
					throw new Exception("doesn't have enough rows: " + sceFile.getName());
				}
				cell = line.split(",");
				if(cell.length != numTimeStep + 1) {
					br.close();
					throw new Exception("bad scenario file: " + sceFile.getName());
				}				
				for(int j=0; j<numTimeStep; ++j) {
					sce[k][j] = Double.parseDouble(cell[j+1]);
				}
			}
			br.close();
			
			mapIndexScenario.put(listIndexNumber.get(i), sce);
		}
		
	}
	
	public void blendScenario() throws Exception {
		listFundNumber = fundMap.getFundNumber();		
		// blend scenarios
		mapFundScenario = new HashMap<Integer, double[][]>();
		for(int k : listFundNumber) {
			double[][] sce = new double[numScenario][numTimeStep];
			for(int i=0; i<numScenario; ++i) {
				for(int j=0; j<numTimeStep; ++j) {
					double dSum = 0.0;
					for(int s=0; s<listIndexNumber.size(); ++s) {						
						dSum += fundMap.get(k)[s] * 
								mapIndexScenario.get(listIndexNumber.get(s))[i][j];
					}
					sce[i][j] = dSum;
					
				}
			}
			mapFundScenario.put(k, sce);
		}
	}
	
	public double[][] getFundScenario(int fundNumber) {		
		return mapFundScenario.get(fundNumber);		
	}
	
	public int getNumScenario() {
		return numScenario;
	}
	
	public int getNumTimeStep() {
		return numTimeStep;
	}
}
