package va.policy;

import java.util.*;
import va.calendar.SDate;

public class Policy {
	public int recordID;
	public double survivorShip;
	public String gender; // M, F
	public String productType;
	public SDate issueDate;
	public SDate matDate; // maturity date
	public SDate birthDate;
	public SDate currentDate; // this policy is aged to this date
	public double age;
	public double baseFee; //MER
	public double riderFee; // 	
	public double gmdbAmt; // GMDB
	public double dbRollUpRate;	
	public double gmwbAmt;
	public double gmwbBalance;
	public double wbRollUpRate;
	public double wbWithdrawalRate;	
	public double gmmbAmt; // Maturity GV
	public double mbRollUpRate;
	public double withdrawal; // withdrawal so far
	public double ttm; // time to maturity, currentDate - issueDate
	
	// fund information
	public int[] vFundNum;
	public double[] vFundValue;
	public double[] vFundFee;
	
	public Policy() {
	}
	
	public Policy clone() {
		Policy res = new Policy();
		res.recordID = recordID;
		res.survivorShip = survivorShip;
		res.gender = gender;
		res.productType = productType;
		res.issueDate = new SDate(issueDate);
		res.matDate = new SDate(matDate);
		res.birthDate = new SDate(birthDate);
		res.currentDate = new SDate(currentDate);
		res.age = age;
		res.baseFee = baseFee;
		res.riderFee = riderFee;
		res.gmdbAmt = gmdbAmt;
		res.dbRollUpRate = dbRollUpRate;
		res.gmwbAmt = gmwbAmt;
		res.gmwbBalance = gmwbBalance;
		res.wbRollUpRate = wbRollUpRate;
		res.wbWithdrawalRate = wbWithdrawalRate;
		res.withdrawal = withdrawal;
		res.gmmbAmt = gmmbAmt;
		res.mbRollUpRate = mbRollUpRate;
		res.ttm = ttm;
		
		res.vFundNum = vFundNum.clone();
		res.vFundValue = vFundValue.clone();
		res.vFundFee = vFundFee.clone();
		
		return res;
	}
			
	public Policy(String[] name, String[] value) {
		Map<String, String> mapValue = new HashMap<String, String>();
		for(int i=0; i<name.length; ++i) {
			mapValue.put(name[i].trim(), value[i]);
		}
		recordID = Integer.parseInt(mapValue.get("recordID"));		
		survivorShip = Double.parseDouble(mapValue.get("survivorShip"));
		gender = mapValue.get("gender");
		productType = mapValue.get("productType");
		issueDate = new SDate();
		issueDate.setDateExcel(Double.parseDouble(mapValue.get("issueDate")));
		matDate = new SDate();
		matDate.setDateExcel(Double.parseDouble(mapValue.get("matDate")));
		birthDate = new SDate();
		birthDate.setDateExcel(Double.parseDouble(mapValue.get("birthDate")));
		currentDate = new SDate();
		currentDate.setDateExcel(Double.parseDouble(mapValue.get("currentDate")));
		age = Double.parseDouble(mapValue.get("age"));
		baseFee = Double.parseDouble(mapValue.get("baseFee"));
		riderFee = Double.parseDouble(mapValue.get("riderFee"));
		gmdbAmt = Double.parseDouble(mapValue.get("gmdbAmt"));
		dbRollUpRate = Double.parseDouble(mapValue.get("dbRollUpRate"));
		gmwbAmt = Double.parseDouble(mapValue.get("gmwbAmt"));
		gmwbBalance = Double.parseDouble(mapValue.get("gmwbBalance"));
		wbRollUpRate = Double.parseDouble(mapValue.get("wbRollUpRate"));
		wbWithdrawalRate = Double.parseDouble(mapValue.get("wbWithdrawalRate"));
		gmmbAmt = Double.parseDouble(mapValue.get("gmmbAmt"));
		mbRollUpRate = Double.parseDouble(mapValue.get("mbRollUpRate"));
		withdrawal = Double.parseDouble(mapValue.get("withdrawal"));
		ttm = Double.parseDouble(mapValue.get("ttm"));
		int numFund = 0;
		for(String key : mapValue.keySet()) {
			if(key.startsWith("FundNum")) {
				numFund++;
			}
		}
		vFundNum = new int[numFund];
		vFundValue = new double[numFund];
		vFundFee = new double[numFund];
		for(int j=0; j<numFund; ++j) {
			vFundNum[j] = Integer.parseInt(mapValue.get(String.format("FundNum%d", j+1)));
			vFundValue[j] = Double.parseDouble(mapValue.get(String.format("FundValue%d", j+1)));
			vFundFee[j] = Double.parseDouble(mapValue.get(String.format("FundFee%d", j+1)));
		}
	}
	
	public String getHeader() {
		StringBuilder sb = new StringBuilder();
		sb.append("recordID,").append("survivorShip,").append("gender,").append("productType,").append("issueDate,");
		sb.append("matDate,").append("birthDate,").append("currentDate,age,").append("baseFee,");
		sb.append("riderFee,").append("gmdbAmt,").append("dbRollUpRate,").append("gmwbAmt,");
		sb.append("gmwbBalance,").append("wbRollUpRate,").append("wbWithdrawalRate,");
		sb.append("gmmbAmt,").append("mbRollUpRate,").append("withdrawal,").append("ttm,");
		int j;		
		for(j=0; j<vFundNum.length-1; ++j) {
			sb.append(String.format("FundNum%d,FundValue%d,FundFee%d,", j+1,j+1,j+1));
		}	
		j = vFundNum.length-1;
		sb.append(String.format("FundNum%d,FundValue%d,FundFee%d", j+1,j+1,j+1));
		
		return sb.toString();
	}
	
	public String getString() {		
		StringBuilder sb = new StringBuilder();		
		sb.append(String.format("%d,%.6f,%s,%s,%.0f,", recordID, survivorShip, gender,productType,issueDate.asExcelDate()));
		if(currentDate == null) {
			sb.append(String.format("%.0f,%.0f,%.0f,%.6f,", matDate.asExcelDate(), birthDate.asExcelDate(),
					0.0,age));
		} else {
			sb.append(String.format("%.0f,%.0f,%.0f,%.6f,", matDate.asExcelDate(), birthDate.asExcelDate(),
					currentDate.asExcelDate(),age));
		}
		sb.append(String.format("%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,", baseFee, riderFee, gmdbAmt,dbRollUpRate,gmwbAmt,gmwbBalance));
		sb.append(String.format("%.6f,%.6f,%.6f,%.6f,", wbRollUpRate, wbWithdrawalRate,gmmbAmt,mbRollUpRate));
		sb.append(String.format("%.6f,%.6f,",withdrawal,ttm));
		int j;		
		for(j=0; j<vFundNum.length-1; ++j) {
			sb.append(String.format("%d,%.6f,%.6f,", vFundNum[j], vFundValue[j], vFundFee[j]));
		}	
		j = vFundNum.length-1;
		sb.append(String.format("%d,%.6f,%.6f", vFundNum[j], vFundValue[j], vFundFee[j]));
		
		return sb.toString();
	
	}			
		
	
}
