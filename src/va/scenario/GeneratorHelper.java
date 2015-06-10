package va.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class GeneratorHelper {
	private String paramFile;
	
	private Map<String, double[]> mapFC;
	private Map<String, String> mapParameters;
	private int numScenario;
	private int numYear;
	private int numStep;
	private String forwardCurveFile;
	private String outputFolder;
	private int seed;
	private double dT;
	private List<String> listIndexName;
	private List<String> listIndexFileName;
	private Map<String, Double> mapCor;
	private Map<String, Double> mapVol;
	private String curveDate;
	
	public GeneratorHelper(String paramFile) {
		this.paramFile = paramFile;
		
	}
	
	public void generate() throws Exception {
		loadParameter();
		loadForwardCurve();
		
		int numIndex = listIndexName.size();
		RealMatrix mCov = new Array2DRowRealMatrix(numIndex, numIndex);
		for(int i=0; i<numIndex; ++i) {
			for(int j=0; j<i; ++j) {
				double dCov = getCor(listIndexName.get(i), listIndexName.get(j)) * 
						getVol(listIndexName.get(i)) * getVol(listIndexName.get(j));
				mCov.setEntry(i, j, dCov);
				mCov.setEntry(j, i, dCov);
			}
			mCov.setEntry(i, i, getVol(listIndexName.get(i)) * getVol(listIndexName.get(i)));
		}
		
		for(String key : mapFC.keySet()) {
			Generator g = new Generator(key, listIndexFileName, mCov, mapFC.get(key), 
					seed, numScenario, numStep, dT, curveDate);
			g.generate();
			g.save(outputFolder);
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
        NodeList nl2 = ((Element)nl.item(0)).getElementsByTagName("parameter");
        for(int i=0; i<nl2.getLength(); ++i) {
        	Node node = nl2.item(i);
        	String name = node.getAttributes().getNamedItem("name").getNodeValue();
        	String value = node.getAttributes().getNamedItem("value").getNodeValue();
        	
        	mapParameters.put(name.toLowerCase(), value);
        }
        
        numScenario = getIntParam("numScenario");     
        numStep = getIntParam("numStep");  
        numYear = getIntParam("numYear");  
        dT = (double) numYear / numStep;
        forwardCurveFile = getParam("forwardCurveFile");
        outputFolder = getParam("outputFolder");
        seed = getIntParam("seed");  
        
        listIndexName = new ArrayList<String>();
        listIndexFileName = new ArrayList<String>();
        nl = doc.getElementsByTagName("Indices");
        if(nl.getLength() != 1) {
        	throw new Exception("Number of Indices tag != 1");
        }
        nl2 = ((Element)nl.item(0)).getElementsByTagName("index");
        for(int i=0; i<nl2.getLength(); ++i) {
        	Node node = nl2.item(i);
        	String name = node.getAttributes().getNamedItem("name").getNodeValue();
        	String value = node.getAttributes().getNamedItem("value").getNodeValue();
        	
        	listIndexName.add(name);
        	listIndexFileName.add(value);
        }
        
        nl = doc.getElementsByTagName("Volatility");
        if(nl.getLength() != 1) {
        	throw new Exception("Number of Volatility tag != 1");
        }
        
        mapVol = new HashMap<String, Double>();
        nl2 = ((Element)nl.item(0)).getElementsByTagName("index");
        for(int i=0; i<nl2.getLength(); ++i) {
        	Node node = nl2.item(i);
        	String name = node.getAttributes().getNamedItem("name").getNodeValue();
        	String value = node.getAttributes().getNamedItem("value").getNodeValue();
        	
        	mapVol.put(name.toLowerCase(), Double.parseDouble(value));
        }
        
        nl = doc.getElementsByTagName("Correlation");
        if(nl.getLength() != 1) {
        	throw new Exception("Number of Correlation tag != 1");
        }
        
        mapCor = new HashMap<String, Double>();
        nl2 = ((Element)nl.item(0)).getElementsByTagName("pair");
        for(int i=0; i<nl2.getLength(); ++i) {
        	Node node = nl2.item(i);
        	String name1 = node.getAttributes().getNamedItem("index1").getNodeValue();
        	String name2 = node.getAttributes().getNamedItem("index2").getNodeValue();
        	String value = node.getAttributes().getNamedItem("value").getNodeValue();
        	
        	mapCor.put(String.format("%s:%s", name1.toLowerCase(), name2.toLowerCase()), 
        			Double.parseDouble(value));
        }
        
	}
	
	protected void loadForwardCurve() throws Exception {
		mapFC = new HashMap<String, double[]>();
		
		BufferedReader br = new BufferedReader(new FileReader(forwardCurveFile));
		String[] name;
		String[] cell;
		String header = br.readLine();
		name = header.split(",");
		for(int j=1; j<name.length; ++j) {
			mapFC.put(name[j], new double[numStep]);
		}
		String line = br.readLine(); // get rid of the second row
		cell = line.split(",");
		curveDate = cell[0];
		for(int i=0; i<numStep; ++i) {
			line = br.readLine();
			if(line == null || line.trim().equals("")) {
				br.close();
				throw new Exception("not enought forward rates");
			}
			cell = line.split(",");
			for(int j=1; j<cell.length; ++j) {
				mapFC.get(name[j])[i] = Double.parseDouble(cell[j]);
			}
		}
		br.close();
	}
	
	protected double getVol(String name) throws Exception {
		String key = name.toLowerCase();
		if(mapVol.containsKey(key)) {
			return mapVol.get(key);
		} else {
			throw new Exception("Cannot find volatility: " + name);
		}
	}
	
	protected double getCor(String name1, String name2) throws Exception {
		String key1 = String.format("%s:%s", name1.toLowerCase(), name2.toLowerCase());
		String key2 = String.format("%s:%s", name2.toLowerCase(), name1.toLowerCase());
		if(mapCor.containsKey(key1)) {
			return mapCor.get(key1);
		} else if(mapCor.containsKey(key2)) {
			return mapCor.get(key2);
		} else {
			throw new Exception("Cannot find volatility: " + name1 + ":" + name2);
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
