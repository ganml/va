package va.montecarlo;

import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

import va.curve.*;
import va.policy.*;
import va.scenario.*;

public class PriceHelper {		
	protected Logger log = Logger.getLogger(PriceHelper.class);
	protected Map<String, String> mapParameters;
	protected int startRecordID;
	protected int endRecordID;
	protected String inforceFile;
	
	protected List<String> listIRShock;
	protected List<String> listScenarioFolder;
	protected List<List<String>> listIndexShockName;
	protected List<Map<String, double[]>> listIndexShock;
	protected List<Map<String, Map<Integer, Double>>> listFundShock;
	
	protected int numIndex;
	protected int numScenario;
	protected int numTimeStep;
	protected FundMap fundMap;
	
	protected Map<String, double[]> mapFC;
	protected MortalityCurve mcfemale;
	protected MortalityCurve mcmale;
	protected Map<String, Scenario> mapScenario;
	
	protected Inforce inforce;
	protected List<Result> listResult;
	protected List<String> listErrorMsg;
	protected List<int[]> listIndex;
		
	protected String paramFile;
	
	protected String aggregateOutputFile;
	protected String seriatimOutputFile;
	protected double totalRunTime;
	protected String operation;
	protected int numBlock;
	protected int numThread;
	
	String newline = System.getProperty("line.separator");
	
	public PriceHelper(String paramFile, String operation) {
		this.paramFile = paramFile;		
		this.operation = operation.trim();
		listErrorMsg = new ArrayList<String>();
		listResult = new ArrayList<Result>();
	}
	
	public void price() throws Exception {
		loadParameter();
		loadInforce();
		listIndex = new ArrayList<int[]>();		
		for(int s=0; s<listIRShock.size(); ++s) {
			for(int j=0; j<listIndexShock.get(s).size(); ++j) {
				for(int i=0; i<inforce.size(); ++i) {
					int[] index = new int[]{i,j,s};
					listIndex.add(index);					
				}
			}
		}
		
		if(operation.equals("price") || operation.equals("value")) {
			loadMortalityCurve();
			loadScenario();			
			doWork();			
		} else if(operation.equals("createrunfile")) {
			
			StringBuilder sb = new StringBuilder();
			sb.append("block,startIndex,endIndex,status,runtime");		
			sb.append(newline);
			int nLen = (int) Math.floor((listIndex.size()+0.5) / numBlock);
			int nRem = listIndex.size() - nLen * numBlock;
			int end = -1;
			for(int k=0; k<numBlock; ++k) {
				int beg = end + 1;
				end = beg + nLen - 1;
				if(nRem > 0) {
					end++;
					nRem--;
				}
				if(end<beg) {
					break;
				}
				sb.append(String.format("%d,%d,%d,0,%s", k+1, beg, end, newline));
				// 0 - new, 1- running, 2-finished, 3-error
			}
			
			PrintWriter out = new PrintWriter(getParam("IVRunFile"));
			out.write(sb.toString());
			out.close();
		} else {
			throw new Exception("unknown operation: " + operation);
		}
	}
	
	protected void doWork() throws Exception {
		while(true) {
			RandomAccessFile file = null;
			FileLock fileLock = null;
	        int beg =0;
	        int end =0;
	        int block = 0;
	        totalRunTime = 0;
	        try {	        	
	        	String IVRunFile = getParam("IVRunFile");
	        	String lockFile = IVRunFile.replaceFirst("[.][^.]+$", "") + ".lock";
	        	file = new RandomAccessFile(lockFile, "rw");
	        	FileChannel fileChannel = file.getChannel();
	        	while(true) {
	        		fileLock = fileChannel.tryLock();
	        		if(fileLock!=null) {
	        			break;
	        		}
	        		Thread.sleep(1000);
	        	}
	        	
	        	StringBuilder sb = new StringBuilder();
	        	BufferedReader br = new BufferedReader(new FileReader(IVRunFile));
	        	String[] cell;
	        	sb.append(br.readLine()).append(newline);
	        	while(true){
	        		String line = br.readLine();
	        		if(line == null || line.trim().equals("")) {
	        			break;
	        		}
	        	    cell = line.split(",");
	        		if(Integer.parseInt(cell[3])==0 && block==0) {
	        			block = Integer.parseInt(cell[0]);
	        			beg = Integer.parseInt(cell[1]);
	        			end = Integer.parseInt(cell[2]);
	        			sb.append(String.format("%s,%s,%s,1,%s", cell[0],cell[1],cell[2], newline));
	        		} else {
	        			sb.append(line);
	        			sb.append(newline);
	        		}
	        	}
	        	br.close();	
	        	
	        	FileWriter outFile = new FileWriter(IVRunFile);
	    		PrintWriter out = new PrintWriter(outFile);
	    		out.print(sb.toString());
	    		out.close();
	        	
	        } finally {	
	        	if(fileLock != null) {
	        		fileLock.release();
	        	}
	        	file.close();
	        }	 
	        
	        if(block==0) {
	        	break;
	        }
	        	        
	        Logger log = Logger.getRootLogger();	        
			log.info(String.format("Processing block %d - %d using %d threads", beg, end, numThread));
								       
			int nLen = (int) Math.floor((end-beg+1.0) / numThread);
			int nRem = end-beg+1-nLen*numThread;
			List<Thread> listThread = new ArrayList<Thread>();
			
			int end2 = beg-1;
			for(int k=0; k<numThread; ++k) {
				int beg2 = end2 + 1;
				end2 = beg2 + nLen - 1;
				if(nRem >0) {
					end2++;
					nRem--;
				}
				if(end2 < beg2) {
					break;
				}
				
				class MyTask implements Runnable {
					private int a;
					private int b;
					public MyTask(int a, int b) {
						this.a = a;
						this.b = b;
					}
				    public void run(){
				       doWork(a,b);
				    }				  
				}
				
				Thread t = new Thread(new MyTask(beg2, end2));
				listThread.add(t);
				t.start();
			}
            
			for(Thread t: listThread) {
				t.join();
			}
			
            log.info(String.format("Runtime used for %d - %d: %f", beg, end, totalRunTime));
			saveResult(block);	        
			listResult.clear();
	        
			try {	        	
	        	String IVRunFile = getParam("IVRunFile");
	        	String lockFile = IVRunFile.replaceFirst("[.][^.]+$", "") + ".lock";
	        	file = new RandomAccessFile(lockFile, "rw");
	        	FileChannel fileChannel = file.getChannel();
	        	while(true) {
	        		fileLock = fileChannel.tryLock();
	        		if(fileLock!=null) {
	        			break;
	        		}
	        		Thread.sleep(1000);
	        	}
	        	
	        	StringBuilder sb = new StringBuilder();
	        	BufferedReader br = new BufferedReader(new FileReader(IVRunFile));
	        	String line = br.readLine();
	        	sb.append(line).append(newline);
	        	while(true) {
	        		line = br.readLine();
	        		if(line == null || line.trim().equals("")) {
	        			break;
	        		}
	        		
	        		String[] cell = line.split(",");
	        		if(Integer.parseInt(cell[0])==block) {	        			
	        			beg = Integer.parseInt(cell[1]);
	        			end = Integer.parseInt(cell[2]);
	        			sb.append(String.format("%s,%s,%s,2,%f", cell[0],cell[1],cell[2], totalRunTime));
	        			sb.append(newline);
	        		} else {
	        			sb.append(line).append(newline);
	        		}
	        	}
	        	br.close();
	        	
	        	FileWriter outFile = new FileWriter(IVRunFile);
	    		PrintWriter out = new PrintWriter(outFile);
	    		out.print(sb.toString());
	    		out.close();
	        } finally {	        	
	        	if(fileLock != null) {
	        		fileLock.release();
	        	}
	        	file.close();
	        }
		}
	}
	
	
	protected void doWork(int beg, int end) {
		List<Result> listLocalResult = new ArrayList<Result>();
		long start = System.currentTimeMillis();
		
		for(int i=beg; i<=end; ++i) {
			int[] index = listIndex.get(i); // record, eqshock, irshock
			String irShock = listIRShock.get(index[2]);
			Policy p = inforce.get(index[0]);
			Pricer pricer = getPricer(p.productType, irShock);
			if(pricer == null) {
				break;
			}
			
			if(operation.equals("price")) {
				double x, x0, x1, f0, f1;
				x0 = 0.0;
				p.riderFee = x0;
				Result res = pricer.price(p, 
						listFundShock.get(index[2]).get(listIndexShockName.get(index[2]).get(index[1])),
						 mapFC.get(irShock), mcfemale, mcmale, fundMap);
				f0 = res.fmv;
				x1 = 0.05;
				p.riderFee = x1;
				res = pricer.price(p, 
						listFundShock.get(index[2]).get(listIndexShockName.get(index[2]).get(index[1])),
						 mapFC.get(irShock), mcfemale, mcmale, fundMap);
				f1 = res.fmv;
				
				while(Math.abs(x1-x0) > 1e-6) {
					x = x0 - f0 * (x1 - x0) / (f1 - f0);
					p.riderFee = x;
					res = pricer.price(p, 
							listFundShock.get(index[2]).get(listIndexShockName.get(index[2]).get(index[1])),
							 mapFC.get(irShock), mcfemale, mcmale, fundMap);
					
					x0 = x1;
					f0 = f1;

					x1 = x;
					f1 = res.fmv;	
				}

				res.irShock = listIRShock.get(index[2]);
				res.eqShock = listIndexShockName.get(index[2]).get(index[1]);
				res.recordID = p.recordID;
				res.riderFee = p.riderFee;
				listLocalResult.add(res);
			} else {
				Result res = pricer.price(p, 
						listFundShock.get(index[2]).get(listIndexShockName.get(index[2]).get(index[1])),
						 mapFC.get(irShock), mcfemale, mcmale, fundMap);
				res.irShock = listIRShock.get(index[2]);
				res.eqShock = listIndexShockName.get(index[2]).get(index[1]);
				res.recordID = p.recordID;
				res.riderFee = p.riderFee;
				listLocalResult.add(res);
			}
			/*
			if(p.recordID==4 && listIndexShockName.get(index[2]).get(index[1]).equals("base")) {
				log.info(String.format("%s, %s", p.currentDate.toString(), p.matDate.toString()));
				try{
					pricer.saveAV();
					pricer.saveDB();
					pricer.saveWB();
					pricer.saveMB();
					pricer.saveRC();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			//*/
		}
		double timeElapsed = (System.currentTimeMillis() - start) / 1000.0;
		
		synchronized(this) {
			listResult.addAll(listLocalResult);
			totalRunTime += timeElapsed;			
		}
	}
	
	protected Pricer getPricer(String productType, String irShock) {
		String className = "va.montecarlo.Pricer" + productType;
		Pricer pricer = null;
		try{
			Constructor<?> cons = Class.forName(className).getConstructor(Scenario.class, int.class); 
			pricer = (Pricer) cons.newInstance(mapScenario.get(irShock), 0);
		}catch (Exception ex) {
			log.error("Cannot find pricer for " + productType);
		}
				
		return pricer;
	}
	
	protected void saveResult(int block) throws Exception {
		String newline = System.getProperty("line.separator");
		
		int numIndex = fundMap.getNumIndex();
		if(seriatimOutputFile.length() >0 ) {
			StringBuilder sb = new StringBuilder();
			sb.append("irShock,eqShock,recordID,fmv,riderFee");
			for(int i=0; i<numIndex; ++i) {
				sb.append(String.format(",Index %d", i+1));
			}
			sb.append(",Total AV");
			sb.append(newline);
			for(int i=0; i<listResult.size(); ++i) {
				Result res = listResult.get(i);
				sb.append(String.format("%s,%s,%d,%.6f,%.6f",res.irShock, res.eqShock, 
						res.recordID, res.fmv, res.riderFee));
				for(int j=0; j<numIndex; ++j) {
					sb.append(String.format(",%.6f", res.av[j]));
				}
				sb.append(String.format(",%.6f", res.av[numIndex]));
				sb.append(newline);
			}
			
			String outputFile = String.format("%s_%d.csv", 
					seriatimOutputFile.replaceFirst("[.][^.]+$", ""), block);
			FileWriter outFile = new FileWriter(outputFile);
			PrintWriter out = new PrintWriter(outFile);
			out.print(sb.toString());
			out.close();
		}
		
		if(aggregateOutputFile.length() > 0) {
			Map<String, Map<String, double[]>> mmRes = new HashMap<String, Map<String, double[]>>();
			
			for(int i=0; i<listIRShock.size(); ++i) {
				Map<String, double[]> mRes = new HashMap<String, double[]>();
				for(String eqShock : listIndexShockName.get(i)) {
					mRes.put(eqShock, new double[numIndex+2]);
				}
				mmRes.put(listIRShock.get(i), mRes);
			}
			
			for(int i=0; i<listResult.size(); ++i) {
				Result res = listResult.get(i);
				
				double[] vTmp = mmRes.get(res.irShock).get(res.eqShock); 
				vTmp[0] += res.fmv;
				for(int j=0; j<numIndex+1; ++j) {
					vTmp[j+1] += res.av[j];
				}
				
				//mmRes.get(res.irShock).put(res.eqShock, vTmp);
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("irShock,eqShock,recordID,fmv");
			for(int i=0; i<numIndex; ++i) {
				sb.append(String.format(",Index %d", i+1));
			}
			sb.append(",Total AV");
			sb.append(newline);
			for(String irShock : mmRes.keySet()) {
				for(String eqShock : mmRes.get(irShock).keySet()) {
					sb.append(String.format("%s,%s,%d",irShock, eqShock, 0));
					for(int j=0; j<numIndex+2; ++j) {
						sb.append(String.format(",%.6f", mmRes.get(irShock).get(eqShock)[j]));
					}
					sb.append(newline);
				}
			}
			
			String outputFile = String.format("%s_%d.csv", 
					aggregateOutputFile.replaceFirst("[.][^.]+$", ""), block);
			FileWriter outFile = new FileWriter(outputFile);
			PrintWriter out = new PrintWriter(outFile);
			out.print(sb.toString());
			out.close();
		}
	}
	
	protected void loadInforce() throws Exception {
		inforce = new Inforce();
		BufferedReader br = new BufferedReader(new FileReader(inforceFile));
		String[] name;
		String[] cell;
		String header = br.readLine();
		name = header.split(",");
		while(true) {
			String line = br.readLine();
			if(line == null || line.trim().equals("")) {
				break;
			}
			cell = line.split(",");
			Policy p = new Policy(name, cell);
			if(p.recordID >= startRecordID && p.recordID <= endRecordID) {
				inforce.addPolicy(p);
			}
		}
		br.close();
	}
	
	protected void loadMortalityCurve() throws Exception {
		mcfemale = new MortalityCurve(getParam("FemaleMortalityFile"));
		mcmale = new MortalityCurve(getParam("MaleMortalityFile"));
	}
	
	protected void loadScenario() throws Exception {
		mapFC = new HashMap<String, double[]>();
		mapScenario = new HashMap<String, Scenario>();
		
		for(int k=0; k<listIRShock.size(); ++k) {
			File folder = new File(listScenarioFolder.get(k));
			File file = new File(folder, getParam("CurveFile"));
			
			double[] vFC = new double[numTimeStep];
			BufferedReader br = new BufferedReader(new FileReader(file));
			br.readLine();
			br.readLine();
			for(int i=0; i<numTimeStep; ++i) {				
				vFC[i] = Double.parseDouble(br.readLine());
			}
			br.close();
			mapFC.put(listIRShock.get(k), vFC);
			
			Scenario sce = new Scenario(listScenarioFolder.get(k), fundMap, numScenario, numTimeStep);
			mapScenario.put(listIRShock.get(k), sce);
		}
	}
	
	protected void loadParameter() throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse (new File(paramFile));
        
        doc.getDocumentElement().normalize();
        NodeList nl = doc.getElementsByTagName("Parameters");
        if(nl.getLength() != 1) {
        	throw new Exception("Number of Parameters tag != 1");
        }
        mapParameters = new HashMap<String, String>();
        NodeList nl2 = ((Element)nl.item(0)).getElementsByTagName("Parameter");
        for(int i=0; i<nl2.getLength(); ++i) {
        	Node node = nl2.item(i);
        	String name = node.getAttributes().getNamedItem("name").getNodeValue();
        	String value = node.getAttributes().getNamedItem("value").getNodeValue();
        	
        	mapParameters.put(name.toLowerCase(), value);
        }
        
        aggregateOutputFile = getParam("AggregateOutputFile").trim();
		seriatimOutputFile = getParam("SeriatimOutputFile").trim();
		
        startRecordID = getIntParam("startRecordID");
        endRecordID = getIntParam("endRecordID");
        inforceFile = getParam("inforceFile");
        
        numIndex = getIntParam("numIndex");
        numScenario = getIntParam("numScenario");
        numTimeStep = getIntParam("numTimeStep");
        numBlock = getIntParam("numBlock");
        numThread = getIntParam("numThread");
        fundMap = new FundMap(getParam("fundMapFile"));
        
        listIRShock = new ArrayList<String>();
        listScenarioFolder = new ArrayList<String>();
        listIndexShockName = new ArrayList<List<String>>();
        listIndexShock = new ArrayList<Map<String, double[]>>();
        listFundShock = new ArrayList<Map<String, Map<Integer, Double>>>();
        nl = doc.getElementsByTagName("IRShock");
        if(nl.getLength() < 1) {
        	throw new Exception("there is no IR shock");
        }
        for(int k=0; k<nl.getLength(); ++k) {
        	String irShockName = nl.item(k).getAttributes().getNamedItem("name").getNodeValue();
        	String scenarioFolder = nl.item(k).getAttributes().getNamedItem("scenario").getNodeValue();
        	listIRShock.add(irShockName);
        	listScenarioFolder.add(scenarioFolder);
        	
        	List<String> listName = new ArrayList<String>();
        	Map<String, double[]> mapShock = new HashMap<String, double[]>();
        	Map<String, Map<Integer, Double>> mapFundShock = new HashMap<String, Map<Integer, Double>>();
        	nl2 = ((Element)nl.item(k)).getElementsByTagName("EquityShock");
            for(int i=0; i<nl2.getLength(); ++i) {
            	String eqShockName = nl2.item(i).getAttributes().getNamedItem("name").getNodeValue();
            	String eqShock = nl2.item(i).getAttributes().getNamedItem("value").getNodeValue();
            
            	listName.add(eqShockName);
            	String[] cell = eqShock.split(",");
            	if(cell.length != numIndex) {
            		throw new Exception("bad equity shock :" + eqShock);
            	}
            	double[] vShock = new double[numIndex];
            	for(int s=0; s<numIndex; ++s) {
            		vShock[s] = Double.parseDouble(cell[s]);
            	}
            	mapShock.put(eqShockName, vShock);
            	mapFundShock.put(eqShockName, fundMap.convert(vShock));
            }
            listIndexShockName.add(listName);
            listIndexShock.add(mapShock);
            listFundShock.add(mapFundShock);
        }
        
	}
	
	protected int getIntParam(String name) throws Exception {
		String key = name.toLowerCase();
		if(mapParameters.containsKey(key)) {
			return Integer.parseInt(mapParameters.get(key));
		} else {
			throw new Exception("Cannot find parameter: " + name);
		}
	}
	
	protected String getParam(String name) throws Exception {
		String key = name.toLowerCase();
		if(mapParameters.containsKey(key)) {
			return mapParameters.get(key);
		} else {
			throw new Exception("Cannot find parameter: " + name);
		}
	}
}
