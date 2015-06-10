package va.curve;

import va.calendar.*;
import va.calendar.DateGenerator.*;

import java.util.*;

import org.apache.log4j.Logger;

public class Swap extends Instrument {
	protected Logger log = Logger.getLogger(Swap.class.getName());
	protected double notionalAmount;	
	protected int numSetDays;
	protected int numYears;
	protected double swapRate;
	protected List<SDate> lstPaymentDate;
	
	public Swap(int numYears, SDate startDate, int numSetDays, HolidayCalendar hc, ZeroCurve zc, DayCountBasis dcb) {
		this.numSetDays = numSetDays;
		this.numYears = numYears;
		this.startDate = startDate;
		this.hc = hc;
		this.zc = zc;
		this.dcb = dcb;
		
		init();
	}
	
	public void setSwapRate(double swapRate) {
		this.swapRate = swapRate;
	}
	
	public double getSwapRate() {
		return swapRate;
	}
	
	public int getNumYears() {
		return numYears;
	}
			
	public double df() throws Exception {
		double x=0, x0, x1, f, f0, f1;
		x0 = Math.exp(-swapRate * numYears);
		f0 = npv(x0);
		//log.info(String.format("%f, %f", x0, f0));
		x1 = x0 + 1e-3;
		f1 = npv(x1);
		//log.info(String.format("%f, %f", x1, f1));
		while(Math.abs(x1-x0) > 1e-10) {			
			x = x0 - f0 * (x1 - x0) / (f1 - f0);
			f = npv(x);

			x0 = x1;
			f0 = f1;

			x1 = x;
			f1 = f;	
			//log.info(String.format("%f, %f", x, f));
		}

		return x;
	}
	
	protected void init() {
		int nPayments = numYears*2;
		lstPaymentDate = new ArrayList<SDate>();
		
		DateGenerator dg = new DateGenerator();
		setDate = startDate;
		for(int i=0; i<numSetDays; ++i) {
			setDate = dg.dateAdd(setDate, 1, RollConvention.FOLLOW, Frequency.DAILY, hc);
		}
		
		SDate temp;
		for(int j=0; j<nPayments; ++j) {
			temp = dg.dateAdd(setDate, 6*(j+1), RollConvention.MOD_FOLLOW, Frequency.MONTHLY, hc);
			lstPaymentDate.add(temp);
		}
		matDate = lstPaymentDate.get(nPayments-1);
	}

	protected double npv(double guess) throws Exception {
		ZeroCurve zcNew = zc.clone();
		zcNew.add(matDate, guess);

		int nPayments = lstPaymentDate.size();
		double[] fix = new double[nPayments];
		double[] flt = new double[nPayments];

		double tfrac = dcb.tfrac(setDate, lstPaymentDate.get(0));
		double f = (zcNew.loglinear(setDate)/zcNew.loglinear(lstPaymentDate.get(0))-1) / (tfrac);
		fix[0] = swapRate * tfrac;
		flt[0] = f * tfrac;
		
		for(int i=1; i<nPayments; ++i) {
			tfrac = dcb.tfrac(lstPaymentDate.get(i-1), lstPaymentDate.get(i));
			f = (zcNew.loglinear(lstPaymentDate.get(i-1))/zcNew.loglinear(lstPaymentDate.get(i))-1) / (tfrac);
			fix[i] = swapRate * tfrac;
			flt[i] = f * tfrac;			
		}

		double temp = 0.0;
		for(int i=0; i<nPayments; ++i) {
			temp += (fix[i]-flt[i]) * zcNew.loglinear(lstPaymentDate.get(i));
		}

		return temp;
	}
}
