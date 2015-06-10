package va.scenario;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public class Generator {
	private String irShockName;
	private List<String> listIndexFileName;
	private RealMatrix mCov; // covariance matrix
	private double[] vFC; // forward curve
	private double dT;
	private int numScenario;
	private int numStep;
	private int numIndex;
	private MersenneTwister mt;
	private List<double[][]> listScenario;
	private String curveDate;
	
	private double[][] mStat; // scenario statistics

	public Generator(String irShockName, List<String> listIndexFileName, RealMatrix mCov, 
			double[] vFC, int seed, int numScenario, int numStep, double dT, String curveDate) {
		this.irShockName = irShockName;
		this.listIndexFileName = listIndexFileName;
		this.mCov = mCov;
		this.vFC = vFC;
		this.dT = dT;
		this.numScenario = numScenario;
		this.numStep = numStep;
		this.curveDate = curveDate;
		
		this.numIndex = listIndexFileName.size();
		
		mt = new MersenneTwister(seed);
	}
	
	public void generate() {
		CholeskyDecomposition cd = new CholeskyDecomposition(mCov);
		double[][] L = cd.getL().getData();
				
		listScenario = new ArrayList<double[][]>();
		for(int k=0; k<numIndex; ++k) {
			listScenario.add(new double[numScenario][numStep]);
		}

		double[] vTemp = new double[numIndex];
		for(int i=0; i<numScenario; ++i) {
			for(int j=0; j<numStep; ++j) {
				for(int l=0; l<numIndex; ++l) {
					vTemp[l] = mt.nextGaussian();
				}				
				for(int h=0; h<numIndex; ++h) {
					double dSum = vFC[j] * dT;
					for(int l=0; l<numIndex; ++l) {
						dSum += -L[h][l]*L[h][l] * dT / 2.0 + L[h][l] * Math.sqrt(dT) * vTemp[l];
					}
					listScenario.get(h)[i][j] = dSum;
				}
			}
		}
		
		// calculate statistics
		StandardDeviation sd = new StandardDeviation();
		PearsonsCorrelation pc = new PearsonsCorrelation();
		Mean sm = new Mean();
		
		mStat = new double[numIndex+2][numIndex];
		for(int i=0; i<numScenario; ++i) {
			for(int h=0; h<numIndex; ++h) {
				mStat[0][h] += sm.evaluate(listScenario.get(h)[i])/dT;
				mStat[1][h] += sd.evaluate(listScenario.get(h)[i])/Math.sqrt(dT);
			}
			for(int h=0; h<numIndex; ++h) {
				for(int l=0; l<h; ++l) {
					mStat[h+2][l] += pc.correlation(listScenario.get(h)[i], listScenario.get(l)[i]);
					mStat[l+2][h] = mStat[h+2][l];
				}
				mStat[h+2][h] += 1;
			}
		}
		for(int h=0; h<numIndex+2; ++h) {
			for(int l=0; l<numIndex; ++l) {
				mStat[h][l] /= numScenario;
			}
		}
	}

	public void save(String outputFolder) throws Exception {
		File root = new File(outputFolder);
		if(!root.exists()) {
			if(!root.mkdir()) {
				throw new Exception("Cannot create folder: " + root.getPath());
			}
		}
		File folder = new File(root, irShockName);
		if(!folder.exists()) {
			if(!folder.mkdir()) {
				throw new Exception("Cannot create folder: " + folder.getPath());
			}
		}
			
		StringBuilder sb;
		String newline = System.getProperty("line.separator");
		for(int k=0; k<listIndexFileName.size(); ++k) {
			sb = new StringBuilder();
			for(int i=0; i<numScenario; ++i) {
				sb.append("1");
				for(int j=0; j<numStep; ++j) {
					sb.append(String.format(",%.6f", Math.exp(listScenario.get(k)[i][j])));
				}
				sb.append(newline);
			}
			
			File file = new File(folder, listIndexFileName.get(k));
			FileWriter outFile = new FileWriter(file);
			PrintWriter out = new PrintWriter(outFile);
			out.print(sb.toString());
			out.close();
		}
		
		sb = new StringBuilder();
		sb.append("indexNum,indexFile").append(newline);
		for(int k=0; k<listIndexFileName.size(); ++k) {
			sb.append(String.format("%d,%s", k+1, listIndexFileName.get(k)));
			sb.append(newline);
		}
		File file = new File(folder, "indexMap.csv");
		FileWriter outFile = new FileWriter(file);
		PrintWriter out = new PrintWriter(outFile);
		out.print(sb.toString());
		out.close();
		
		sb = new StringBuilder();
		sb.append(irShockName).append(newline);
		sb.append(curveDate).append(newline);
		for(int i=0; i<numStep; ++i) {
			sb.append(vFC[i]).append(newline);
		}
		file = new File(folder, "ForwardCurve.csv");
		outFile = new FileWriter(file);
		out = new PrintWriter(outFile);
		out.print(sb.toString());
		out.close();
		
		sb = new StringBuilder();
		sb.append("Mean");
		for(int k=0; k<listIndexFileName.size(); ++k) {
			sb.append(String.format(",%.6f", mStat[0][k]));			
		}
		sb.append(newline);
		sb.append("Volatility");
		for(int k=0; k<listIndexFileName.size(); ++k) {
			sb.append(String.format(",%.6f", mStat[1][k]));			
		}
		sb.append(newline);		
		for(int k=0; k<listIndexFileName.size(); ++k) {
			sb.append(listIndexFileName.get(k));
			for(int h=0; h<listIndexFileName.size(); ++h) {
				sb.append(String.format(",%.6f", mStat[k+2][h]));
			}
			sb.append(newline);
		}
		
		file = new File(folder, "indexStatistics.csv");
		outFile = new FileWriter(file);
		out = new PrintWriter(outFile);
		out.print(sb.toString());
		out.close();
		
		
	}

}
