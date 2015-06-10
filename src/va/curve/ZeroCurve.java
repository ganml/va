package va.curve;

import java.util.*;

import org.apache.log4j.Logger;
import va.calendar.*;

public class ZeroCurve {
	protected Logger log = Logger.getLogger(ZeroCurve.class.getName());
	protected DayCountBasis dcb;
	protected SDate origin;
	protected List<SDate> x;
	protected List<Double> y;
	protected HolidayCalendar hc;
		
	public ZeroCurve(SDate origin, DayCountBasis dcb, HolidayCalendar hc) {
		this.origin = origin;
		this.dcb = dcb;
		this.hc = hc;
		x = new ArrayList<SDate>();
		y = new ArrayList<Double>();
	}
	
	public SDate getCurveDate() {
		return origin;
	}
	
	public ZeroCurve clone() {
		ZeroCurve zc = new ZeroCurve(origin, dcb, hc);
		
		try{
			for(int i=0; i<x.size(); ++i) {
				zc.add(x.get(i), y.get(i));
			}
		} catch(Exception ex) {
			log.error("ZeroCurve clone", ex);
			return null;
		}
		
		return zc;
	}
	
	public void add(SDate date, double df) throws Exception {
		if(x.isEmpty()) {
			if (date.before(origin)) {
				throw new Exception("input date is older than origin");
			}
		} else {
			if (date.before(x.get(x.size()-1))) {				
				throw new Exception("input date is older than the last");
			}
		}
		x.add(date);
		y.add(df);

	}
	
	public double loglinear(SDate d) {
		if(d.before(origin)) {
			return 0.0;
		}

		if(d.equals(origin) || x.isEmpty()) {
			return 1.0;
		}

		for(int i=0; i<x.size(); ++i) {
			if(x.get(i).equals(d)) {
				return y.get(i);
			}
		}

		SDate d1=null, d2=null;
		double df1=0, df2=0;

		boolean bTag = false;
		if(d.after(origin) && d.before(x.get(0))) {
			d1 = origin; df1 = 1;
			d2 = x.get(0); df2 = y.get(0);
			bTag = true;
		} else {
			for(int i=0; i<x.size()-1; ++i) {
				if( d.after(x.get(i)) && d.before(x.get(i+1))) {
					d1 = x.get(i); df1 = y.get(i);
					d2 = x.get(i+1); df2 = y.get(i+1);
					bTag = true;
				}
			}
		}
		
		if(!bTag) {
			if (x.size() == 1 ){
				d1 = origin; df1 = 1;
				d2 = x.get(0); df2 = y.get(0);
			} else {
				d1 = x.get(x.size()-2); df1 = y.get(x.size()-2);
				d2 = x.get(x.size()-1); df2 = y.get(x.size()-1);
			}
		}
				
		double t1 = dcb.tfrac(origin,d1);
		double t2 = dcb.tfrac(origin,d2);
		double t = dcb.tfrac(origin,d);
		double temp = (Math.log(df2) - Math.log(df1))*t - Math.log(df2) * t1 + Math.log(df1)*t2;

		return Math.exp( temp / (t2 - t1) );
	}
	
	public double get(SDate d) {
		if(d.equals(origin) ) {
			return 1.0;
		}

		for(int i=0; i<x.size(); ++i) {
			if(x.get(i).equals(d)) {
				return y.get(i);
			}
		}

		return -1.0;
	}

	public double fr(SDate d1, SDate d2) {
		if(d1.equals(d2)) {
			return 0.0;
		} else {
			return ( loglinear(d1) / loglinear(d2) - 1) / 
			(dcb.tfrac(origin,d2) - dcb.tfrac(origin,d1));
		}
	}
	
	public double fr(SDate d1, SDate d2, DayCountBasis dcbi) {
		if(d1.equals(d2)) {
			return 0.0;
		} else {
			return ( loglinear(d1) / loglinear(d2) - 1) / 
			(dcbi.tfrac(origin,d2) - dcbi.tfrac(origin,d1));
		}
	}
	
	public double zr(SDate d, boolean cc) {
		if (cc) {
			return -Math.log( loglinear(d)) / dcb.tfrac(origin,d);
		} else {
			return Math.pow(loglinear(d), -1/dcb.tfrac(origin,d)) - 1.0;
		}
	}
	
	public String toString() {
		String linesep = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("Date,Discount Factor,Forward Rate,Zero Rate").append(linesep);
		sb.append(origin.toString()).append(",").append(get(origin)).append(",");
		sb.append("0,0").append(linesep);
		for(int i=0; i<x.size(); ++i) {
			sb.append(x.get(i).toString()).append(",").append(get(x.get(i))).append(",");
			if(i==0) {
				sb.append(fr(origin, x.get(i)));
			} else {
				sb.append(fr(x.get(i-1),x.get(i)));
			}
			sb.append(",").append(zr(x.get(i),true)).append(linesep);
		}
		
		return sb.toString();
	}
}
