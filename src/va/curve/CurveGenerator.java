package va.curve;

import java.io.*;
import java.util.*;
import va.calendar.*;
import va.calendar.DateGenerator.*;
//import va.calendar.DayCountBasis.BasisType;

public class CurveGenerator {
	protected String inputFileName;
	protected ZeroCurve zc;
	protected HolidayCalendar hc;
	protected boolean bCalculated;
	protected List<Instrument> lstInstrument;
		
	protected List<String> lstForwardName;
	protected List<ZeroCurve> lstZeroCurve;
	
	public CurveGenerator(String inputFileName) {
		this.inputFileName = inputFileName;
		bCalculated = false;
	}
			
	public ZeroCurve getZeroCurve() throws Exception {
		if(!bCalculated) {
			readCurveData();
			constructCurve();
			bCalculated = true;
		}
		return zc;
	}
	
	public void saveForwardCurve(String filename, Frequency freq, int years) throws Exception {
		if(!bCalculated) {
			readCurveData();
			constructCurve();
			bCalculated = true;
		}
		
		DateGenerator dg = new DateGenerator();
		List<SDate> lstDate = dg.createDates(zc.getCurveDate(), years, RollConvention.MOD_FOLLOW, freq, hc);
		
		//DayCountBasis dcb = new DayCountBasis(BasisType.ACTACT);
		String linesep = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("Date");
		for(int i=0; i<lstForwardName.size(); ++i) {
			sb.append(',').append(lstForwardName.get(i));
		}
		sb.append(linesep);
		sb.append(lstDate.get(0).toString());
		for(int i=0; i<lstForwardName.size(); ++i) {
			sb.append(',').append(0.0);
		}
		sb.append(linesep);
		for(int k=1; k<lstDate.size(); ++k) {
			sb.append(lstDate.get(k).toString());
			for(int i=0; i<lstForwardName.size(); ++i) {
				sb.append(',').append( lstZeroCurve.get(i).fr(lstDate.get(k-1), lstDate.get(k)) );
			}
			sb.append(linesep);
		}
		
		FileWriter outFile = new FileWriter(filename);
		PrintWriter out = new PrintWriter(outFile);
		out.print(sb.toString());
		out.close();
	}

	protected void readCurveData() throws Exception {
		//System.out.println("Working Directory = " + System.getProperty("user.dir"));
		//System.out.println(new File(".").getAbsolutePath());
		BufferedReader br = new BufferedReader(new FileReader(inputFileName));
		String line;
		String[] cell;
		line = br.readLine(); // header
				
		CalendarBroker cb = new CalendarBroker();
		lstInstrument = new ArrayList<Instrument>();
		while(true) {
			line = br.readLine();
			if(line == null || line.trim().equals("")) {
				break;
			}
			cell = line.split(",");
			if(cell.length != 7) {
				br.close();
				throw new Exception("bad curve line: " + line);
			}
			
			int nYears = Integer.parseInt(cell[0]);
			if(!cell[1].trim().toLowerCase().equals("y")) {
				br.close();
				throw new Exception("Cannot handle period: " + cell[1]);
			}			
			DayCountBasis dcb = new DayCountBasis(cell[2].trim());
			int nSettleDays = Integer.parseInt(cell[3].trim());
			hc = cb.getCalendar(cell[4].trim());
			SDate startDate = new SDate(cell[5].trim());
			double swapRate = Double.parseDouble(cell[6].trim()) / 100.0;
			
			Swap swap = new Swap(nYears, startDate, nSettleDays, hc, null, dcb);
			swap.setSwapRate(swapRate);
			lstInstrument.add(swap);
		}
		
		br.close();
		
	}
	
	protected void constructCurve() throws Exception {
		lstForwardName = new ArrayList<String>();
		lstZeroCurve = new ArrayList<ZeroCurve>();
		
		double[] rates = new double[lstInstrument.size()];
		for(int i=0; i<lstInstrument.size(); ++i) {
			rates[i] = ((Swap) lstInstrument.get(i)).getSwapRate();
		}
		
		SDate origin;
		DayCountBasis dcb;
		for(int k=0; k<lstInstrument.size(); ++k) {
			// up shock
			origin = lstInstrument.get(0).getStartDate();
			dcb = lstInstrument.get(0).getDayCountBasis();
			zc = new ZeroCurve(origin, dcb, hc);
			double shock = 0.0;
			for(int i=0; i<lstInstrument.size(); ++i) {
				lstInstrument.get(i).setZeroCurve(zc);
				if(i ==k ) {
					shock = 0.001;
				} else {
					shock = 0.0;
				}
				((Swap)lstInstrument.get(i)).setSwapRate(rates[i] + shock);
				double df = lstInstrument.get(i).df();
				zc.add(new SDate(lstInstrument.get(i).getMatDate()), df);
			}
			
			lstForwardName.add(String.format("%dy_up", ((Swap)lstInstrument.get(k)).getNumYears()));
			lstZeroCurve.add(zc);
			
			// down shock
			origin = lstInstrument.get(0).getStartDate();
			dcb = lstInstrument.get(0).getDayCountBasis();
			zc = new ZeroCurve(origin, dcb, hc);
			shock = 0.0;
			for(int i=0; i<lstInstrument.size(); ++i) {
				lstInstrument.get(i).setZeroCurve(zc);
				if(i ==k ) {
					shock = -0.001;
				} else {
					shock = 0.0;
				}
				((Swap)lstInstrument.get(i)).setSwapRate(rates[i] + shock);
				double df = lstInstrument.get(i).df();
				zc.add(new SDate(lstInstrument.get(i).getMatDate()), df);
			}
			
			lstForwardName.add(String.format("%dy_down", ((Swap)lstInstrument.get(k)).getNumYears()));
			lstZeroCurve.add(zc);
		}
		
		// parallel up
		origin = lstInstrument.get(0).getStartDate();
		dcb = lstInstrument.get(0).getDayCountBasis();
		zc = new ZeroCurve(origin, dcb, hc);
		for(int i=0; i<lstInstrument.size(); ++i) {
			lstInstrument.get(i).setZeroCurve(zc);
			((Swap)lstInstrument.get(i)).setSwapRate(rates[i] + 0.001);
			double df = lstInstrument.get(i).df();
			zc.add(new SDate(lstInstrument.get(i).getMatDate()), df);
		}
		
		lstForwardName.add("all_up");
		lstZeroCurve.add(zc);
		
		// parallel down
		origin = lstInstrument.get(0).getStartDate();
		dcb = lstInstrument.get(0).getDayCountBasis();
		zc = new ZeroCurve(origin, dcb, hc);
		for(int i=0; i<lstInstrument.size(); ++i) {
			lstInstrument.get(i).setZeroCurve(zc);
			((Swap)lstInstrument.get(i)).setSwapRate(rates[i] - 0.001);
			double df = lstInstrument.get(i).df();
			zc.add(new SDate(lstInstrument.get(i).getMatDate()), df);
		}
		
		lstForwardName.add("all_down");
		lstZeroCurve.add(zc);
		
		// base
		origin = lstInstrument.get(0).getStartDate();
		dcb = lstInstrument.get(0).getDayCountBasis();
		zc = new ZeroCurve(origin, dcb, hc);
		for(int i=0; i<lstInstrument.size(); ++i) {
			lstInstrument.get(i).setZeroCurve(zc);
			((Swap)lstInstrument.get(i)).setSwapRate(rates[i]);
			double df = lstInstrument.get(i).df();
			zc.add(new SDate(lstInstrument.get(i).getMatDate()), df);
		}
		
		lstForwardName.add("base");
		lstZeroCurve.add(zc);
		
	}
}
