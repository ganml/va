package va.policy;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.parsers.*;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.log4j.Logger;
import org.w3c.dom.*;

import va.calendar.*;
import va.scenario.*;

public class PolicyGenerator {
	protected Logger log = Logger.getLogger(PolicyGenerator.class.getName());
	protected Map<String, String> mapParameters;
	protected int numPolicy;
	protected SDate birthStartDate;
	protected SDate birthEndDate;
	protected SDate issueStartDate;
	protected SDate issueEndDate;
	protected int minMaturity;
	protected int maxMaturity;
	protected double minAccountValue;
	protected double maxAccountValue;	
	protected double femalePercent;
	protected String outputFile;
	
	protected Map<String, Map<String, Double>> mapGuaranteeTypes;
	protected List<String> listProductType;
	protected List<String> listDBType;
	protected List<String> listRider;
	protected List<Double> listPercent;
	protected List<Double> listCumPercent; // cumulative percent
	
	protected List<Integer> listFundNumber;
	protected List<Double> listFundFee;
	
	protected String paramFile;
	protected int seed;
	protected MersenneTwister mt;
	protected Inforce inforce;
	protected PolicyAger pa;
	
	public PolicyGenerator(String paramFile) {
		this.paramFile = paramFile;	
	}
	
	public void generate() throws Exception {
		loadParameter();
		FundMap fundMap = new FundMap(getParam("fundmapfile"));
		Scenario scenario = new Scenario(getParam("scenariofolder"), fundMap,
				1, getIntParam("numtimestep"));
		pa = new PolicyAger(scenario, issueStartDate);
		
		mt = new MersenneTwister(seed);
		inforce = new Inforce();
		SDate currentDate = getDateParam("currentDate");
		log.info(currentDate.toString());
		for(int i=0; i<numPolicy; ++i) {
			Policy p = generateOne(i+1);			
			pa.agePolicy(p, currentDate);
			inforce.addPolicy(p);
		}
		
		inforce.saveToCSV(outputFile);
		
	}
	
	public Policy generateOne(int recordID) throws Exception {
		Policy p = new Policy();
		p.recordID = recordID;		
		p.survivorShip = 1.0;
		
		// generate gender
		double du = mt.nextDouble();
		if(du<femalePercent) {
			p.gender = "F";
		} else {
			p.gender = "M";
		}
		// generate account value
		du = mt.nextDouble();
		double av = (1-du) * minAccountValue + du * (maxAccountValue);
		
		// generate product type
		du = mt.nextDouble();
		int k=-1;
		for(int i=0; i<listPercent.size(); ++i) {
			if( du>listCumPercent.get(i) && du<= listCumPercent.get(i+1) ) {
				k = i;
				break;
			}
		}
		String rider = listRider.get(k).toLowerCase().trim();
		String dbtype = listDBType.get(k).toLowerCase().trim();
		
		if(dbtype.equals("gmdb1") || dbtype.equals("gmdb2")) {
			p.gmdbAmt = av;
			p.dbRollUpRate = getGuaranteeTypeParam(dbtype,"rolluprate");
			p.riderFee = getGuaranteeTypeParam(dbtype,"riderfee");
		} else {
			p.gmdbAmt = 0.0;
			p.dbRollUpRate = 0.0;
			p.riderFee = 0.0;
		}
		
		p.productType = listProductType.get(k);
		p.baseFee = getDoubleParam("basefee") / 10000.0;						
		if(rider.equals("gmwb1") || rider.equals("gmwb2") || rider.equals("gmwb3")) {		
			p.riderFee += getGuaranteeTypeParam(rider,"riderfee");			
			p.wbRollUpRate = getGuaranteeTypeParam(rider,"rolluprate");
			p.wbWithdrawalRate = getGuaranteeTypeParam(rider,"withdrawalrate");
			p.gmwbAmt = av * p.wbWithdrawalRate;
			p.gmwbBalance = av;
		} else if(rider.equals("gmmb")) {
			p.riderFee += getGuaranteeTypeParam(rider,"riderfee");			
			p.mbRollUpRate = getGuaranteeTypeParam(rider,"rolluprate");
			p.gmmbAmt = av;
		} else if (rider.equals("")) {			
		} else {
			throw new Exception("unknown rider type: " + rider);	
		}
		
		// generate birth date		
		int years = (int)(birthEndDate.year() - birthStartDate.year());
		k = mt.nextInt((int)years);
		SDate date = new SDate(birthStartDate);
		date.setYear(birthStartDate.year()+k);
		p.birthDate = date;		
			
		// generate issue date
		years = (int)(issueEndDate.year() - issueStartDate.year());
		
		if(years == 0) {
			k = 0;
		} else {
			k = mt.nextInt((int)years);
		}
		date = new SDate(issueStartDate);
		date.setYear(k + issueStartDate.year());
		p.issueDate = date;
		p.currentDate = p.issueDate;
		p.age = (p.currentDate.julianDate() - p.birthDate.julianDate()) / 365;
		
		// generate maturity date
		years = maxMaturity - minMaturity;
		if(years == 0) {
			k = minMaturity;
		} else {
			k = mt.nextInt(years) + minMaturity;
		}
		date = new SDate(p.issueDate);
		date.setYear(p.issueDate.year() + k);
		p.matDate = date;		
		
		// generate funds
		int[] a = new int[listFundNumber.size()];
		for (int i = 0; i < listFundNumber.size(); i++)
			a[i] = i;

		// shuffle
		for (int i = 0; i < listFundNumber.size(); i++) {
			int r = mt.nextInt(listFundNumber.size());     // int between 0 and i
			int swap = a[r];
			a[r] = a[i];
			a[i] = swap;
		}
	      
		p.vFundNum = new int[listFundNumber.size()];
		p.vFundFee = new double[listFundNumber.size()];
		for(int i=0; i<listFundNumber.size(); ++i) {
			p.vFundNum[i] = listFundNumber.get(i);
			p.vFundFee[i] = listFundFee.get(i);
		}
		p.vFundValue = new double[listFundNumber.size()];
		int count = mt.nextInt(listFundNumber.size()) + 1;
		
		for(int i=0; i<count;++i) {
			k = a[i];				
			p.vFundValue[k] = av / count;			
		}

		for(int i=0; i<p.vFundNum.length; ++i) {
			if(p.vFundValue[i] <0) {
				throw new Exception(String.format("negative fund value %d, %f", p.vFundNum[i], p.vFundValue[i]));	
			}
		}
		return p;
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
        numPolicy = getIntParam("numPolicy");     
        birthStartDate = getDateParam("birthStartDate");
        birthEndDate = getDateParam("birthEndDate");
        issueStartDate = getDateParam("issueStartDate");
        issueEndDate = getDateParam("issueEndDate");
        minMaturity = getIntParam("minMaturity");
        maxMaturity = getIntParam("maxMaturity");
        minAccountValue = getDoubleParam("minAccountValue");
        maxAccountValue = getDoubleParam("maxAccountValue");        
        seed = getIntParam("seed");
        femalePercent = getDoubleParam("femalePercent");
        outputFile = getParam("outputFile");
        
        mapGuaranteeTypes = new HashMap<String, Map<String, Double>>();
        nl = doc.getElementsByTagName("GuaranteeTypes");
        if(nl.getLength() != 1) {
        	throw new Exception("Number of GuaranteeTypes tag != 1");
        }
        nl2 = ((Element)nl.item(0)).getElementsByTagName("GuaranteeType");
        for(int i=0; i<nl2.getLength(); ++i) {
        	Node node = nl2.item(i);
        	String name = node.getAttributes().getNamedItem("name").getNodeValue();
        	NodeList nl3 = ((Element)node).getElementsByTagName("Parameter");
        	Map<String, Double> map = new HashMap<String, Double>();
        	for(int j=0; j<nl3.getLength(); ++j) {
        		Node node2 = nl3.item(j);
        		String name2 = node2.getAttributes().getNamedItem("name").getNodeValue();
        		String value = node2.getAttributes().getNamedItem("value").getNodeValue();
        		map.put(name2.toLowerCase(), Double.parseDouble(value)/10000.0);
        	}
        	
        	mapGuaranteeTypes.put(name.toLowerCase(), map);
        }
        
        listProductType = new ArrayList<String>();
        listRider = new ArrayList<String>();
        listDBType = new ArrayList<String>();
        listPercent = new ArrayList<Double>();
        listCumPercent = new ArrayList<Double>();
        nl = doc.getElementsByTagName("ProductTypes");
        nl2 = ((Element)nl.item(0)).getElementsByTagName("ProductType");
        for(int i=0; i<nl2.getLength(); ++i) {
        	Node node = nl2.item(i);
        	String name = node.getAttributes().getNamedItem("name").getNodeValue();
        	String rider = node.getAttributes().getNamedItem("rider").getNodeValue();        	
        	String value = node.getAttributes().getNamedItem("value").getNodeValue();
        	
        	String[] cell = rider.split(":");
        	listProductType.add(name);
        	if(cell.length == 1) {
        		if(!cell[0].toLowerCase().startsWith("gmdb")) {
		        	listRider.add(cell[0]);
		        	listDBType.add("");
        		} else {
        			listRider.add("");
		        	listDBType.add(cell[0]);
        		}
        	} else {
        		if(!cell[0].toLowerCase().startsWith("gmdb")) {
		        	listRider.add(cell[0]);
		        	listDBType.add(cell[1]);
        		} else {
        			listRider.add(cell[1]);
		        	listDBType.add(cell[0]);
        		}
        	}
        	listPercent.add(Double.parseDouble(value));        	
        }
        
        listCumPercent.add(0.0);
        for(int i=0; i<listPercent.size(); ++i) {
        	listCumPercent.add(listCumPercent.get(i) + listPercent.get(i));
        }
        if(Math.abs(listCumPercent.get(listPercent.size()) - 1.0) > 1.0e-4) {
        	throw new Exception("The sum of product percents != 1");
        }

        listFundNumber = new ArrayList<Integer>();
        listFundFee = new ArrayList<Double>();
        nl = doc.getElementsByTagName("Funds");
        nl2 = ((Element)nl.item(0)).getElementsByTagName("Fund");
        for(int i=0; i<nl2.getLength(); ++i) {
        	Node node = nl2.item(i);
        	String number = node.getAttributes().getNamedItem("number").getNodeValue();
        	String fee = node.getAttributes().getNamedItem("fee").getNodeValue();        	
        	
        	listFundNumber.add(Integer.parseInt(number));
        	listFundFee.add(Double.parseDouble(fee) / 10000.0);
        }
	}
	
	protected double getDoubleParam(String name) throws Exception {
		String key = name.toLowerCase();
		if(mapParameters.containsKey(key)) {
			return Double.parseDouble(mapParameters.get(key));
		} else {
			throw new Exception("Cannot find parameter: " + name);
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

	protected SDate getDateParam(String name) throws Exception {
		String key = name.toLowerCase();
		if(mapParameters.containsKey(key)) {
			SimpleDateFormat df = new SimpleDateFormat("dd/M/yyyy");
			Calendar cal = Calendar.getInstance();
			cal.setTime(df.parse(mapParameters.get(key)));
			return new SDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
		} else {
			throw new Exception("Cannot find parameter: " + name);
		}
	}
	
	protected double getGuaranteeTypeParam(String gtype, String pname) throws Exception {
		String key1 = gtype.toLowerCase();
		String key2 = pname.toLowerCase();
		if(mapGuaranteeTypes.containsKey(key1)) {
			Map<String, Double> map = mapGuaranteeTypes.get(key1);
			if(map.containsKey(key2)) {
				return map.get(key2);
			} else {
				throw new Exception("Cannot find GuaranteeType value: " + gtype + ":" + pname);
			}
		} else {
			throw new Exception("Cannot find GuaranteeType: " + gtype);
		}
	}
}
