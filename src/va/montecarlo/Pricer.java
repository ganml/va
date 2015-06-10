package va.montecarlo;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.log4j.Logger;

import va.curve.*;
import va.policy.Policy;
import va.scenario.FundMap;
import va.scenario.Scenario;

public abstract class Pricer {
	protected String name;
	protected Logger log = Logger.getLogger(Pricer.class);
	protected Scenario scenario;
	protected int scenarioOffset;
	
	protected int numScenario;
	protected int numTimeStep;
	
	//cash flow matrix
	protected double[][] AV;// account value after all events
	protected double[][] DB;
	protected double[][] WB;
	protected double[][] MB;
	protected double[][] RC; // risk charge
	
	protected double[] df;
	protected double[] q;
	protected double[] s;
	
	
	public Pricer(Scenario scenario, int scenarioOffset) {
		this.scenario = scenario;
		this.scenarioOffset = scenarioOffset;

		numScenario = scenario.getNumScenario();
		numTimeStep = scenario.getNumTimeStep();
		AV = new double[numScenario][numTimeStep];
		DB = new double[numScenario][numTimeStep];
		WB = new double[numScenario][numTimeStep];
		MB = new double[numScenario][numTimeStep];
		RC = new double[numScenario][numTimeStep];
		df = new double[numTimeStep+1];
		q = new double[numTimeStep+1];
		s = new double[numTimeStep+1];
				
	}
	
	public Result price(Policy po, Map<Integer, Double> shock, double[] fc, MortalityCurve mcfemale, 
			MortalityCurve mcmale, FundMap fundMap) {
		Policy p = po.clone();
		Result res = new Result();
				
		int numIndex = fundMap.getNumIndex();
		double[] av = new double[numIndex + 1];
		for(int i=0; i<numIndex; ++i) {
			double dSum = 0.0;
			for(int k=0; k<p.vFundNum.length; ++k) {
				dSum += p.vFundValue[k] * ( 1 + shock.get(p.vFundNum[k])) * fundMap.get(p.vFundNum[k])[i];
			}
			av[i] = dSum;
			av[numIndex] += dSum;
		}
		res.av = av;
		
		//int numTimeStep = scenario.getNumTimeStep();
		int T = Math.min(p.matDate.year() - p.currentDate.year(), numTimeStep);		
		
		if(T<0) {
			return res;
		}
				
		df[0] = 1.0;
		q[0] = 0.0;
		s[0] = 1.0;
		
		for(int j=1; j<numTimeStep+1; ++j) {					
			df[j] = df[j-1] * Math.exp(-fc[j-1]);			
		}
		if(p.gender.toLowerCase().equals("f")) {
			for(int j=1; j<numTimeStep+1; ++j) {					
				q[j] = mcfemale.q(p.age + (j-1), 1);
				s[j] = mcfemale.p(p.age, j);
			}
		} else {
			for(int j=1; j<numTimeStep+1; ++j) {				
				q[j] = mcmale.q(p.age + (j-1), 1);
				s[j] = mcmale.p(p.age, j);						
			}
			
		}
		
		for(int i=0; i<numScenario; ++i) {
			Policy pc = p.clone();
			for(int k=0; k<pc.vFundNum.length; ++k) {
				pc.vFundValue[k] *= 1 + shock.get(pc.vFundNum[k]);
			}
			for(int j=0; j<numTimeStep; ++j) {
				project(pc, i, j);
			}
		}
				
		double pvRC = 0.0;
		double pvDB = 0.0;
		double pvWB = 0.0;
		double pvMB = 0.0;
		for(int j=1; j<numTimeStep+1; ++j) {
			double dRC = 0.0;
			double dDB = 0.0;
			double dWB = 0.0;
			double dMB = 0.0;
			for(int i=0; i<numScenario; ++i) {
				dRC += RC[i][j-1];
				dDB += DB[i][j-1];
				dWB += WB[i][j-1];
				dMB += MB[i][j-1];
			}
			pvRC += s[j] * dRC * df[j] / numScenario;
			pvDB += s[j-1] * dDB * q[j] * df[j] / numScenario;
			pvWB += s[j] * dWB * df[j] / numScenario;
			pvMB += s[j] * dMB * df[j] / numScenario;
		}
				
		res.riskCharge = pvRC * p.survivorShip;
		res.gmdb = pvDB * p.survivorShip;
		res.gmwb = pvWB * p.survivorShip;
		res.gmmb = pvMB * p.survivorShip;
		res.fmv = (pvDB + pvWB + pvMB - pvRC) * p.survivorShip;
		return res;
	}
	
	public abstract void project(Policy p, int sceInd, int timeInd);

	protected void save(String filename, double[][] M) throws IOException {
		String separator = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<M.length; ++i) {
			sb.append(M[i][0]);
			for(int k=1; k<M[0].length; ++k) {
				sb.append(',').append(M[i][k]);
			}
			sb.append(separator);
		}
		
		FileWriter outFile = new FileWriter(filename);
		PrintWriter out = new PrintWriter(outFile);
		out.print(sb.toString());
		out.close();

	}
	
	public void saveAV() throws IOException {
		save("AV.csv", AV);
	}
	
	public void saveDB() throws IOException {
		save("DB.csv", DB);
	}
	
	public void saveWB() throws IOException {
		save("WB.csv", WB);
	}
	
	public void saveMB() throws IOException {
		save("MB.csv", MB);
	}
	
	public void saveRC() throws IOException {
		save("RC.csv", RC);
	}
}

/*
int numFund = p.vFundNum.length;		

double[][] AVm = new double[numFund][T+1]; // account value at t^-
double[][] AVp = new double[numFund][T+1]; // account value at t^+	
double[] charge = new double[T+1];
double[] db = new double[T+1]; // db guarantee

for(int k=0; k<numFund; ++k) {			
	AVp[k][0] = p.vFundValue[k] * (1 + shock.get(p.vFundNum[k]));			
}
int m = p.issueDate.monthsBetween(p.currentDate);
for(int i=0; i<numScenario; ++i) {
	double gmdbAmt = p.gmdbAmt;
	for(int j=0; j<T; ++j) {
		double dAV = 0.0;
		for(int k=0; k<numFund; ++k) {					
			AVm[k][j+1] = AVp[k][j] * 
					scenario.getFundScenario(p.vFundNum[k])[i][j+scenarioOffset];
			AVp[k][j+1] = AVm[k][j+1] * (1-p.vFundFee[k] / 12);
			double dTemp = AVm[k][j+1] * (1-p.vFundFee[k] / 12) * p.baseFee / 12;
			charge[j+1] += (dTemp-charge[j+1]) / (i+1.0);
			AVp[k][j+1] -= dTemp;
			dAV += AVp[k][j+1];
			
		}
		if( (m+j+1) % 12 ==0 ) {
			gmdbAmt *= 1+p.dbRollUpRate;
		}
		db[j+1] += (Math.max(0, gmdbAmt-dAV) - db[j+1]) / (i+1.0);
	}
}
*/