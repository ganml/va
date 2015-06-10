package va.calendar;

import java.util.*;

public class DateGenerator {	  
	public enum Frequency { 
		ZERO, 
		ANNUAL, 
		SEMIANNUAL, 
		QUARTERLY, 
		MONTHLY, 
		BIWEEKLY, 
		WEEKLY, 
		DAILY
	};		
		  
	public enum RollConvention {
		UNADJUSTED, // do nothing
		PRECEDING, // 1st biz day before holiday
		MOD_PRECEDING, // .. unless it belongs to a different month
		FOLLOW, // 1st biz day following holiday
		MOD_FOLLOW, // .. unless it belongs to a different month
		EOM, // last business day of the current month
		EOM_FOLLOW, // if the last day of the month is a holiday, roll forward
		BOM // first business day of the current month
	};
	
	public SDate addMonths(SDate d1, int months) {
		// to EOM if necc.
	    int m,d,y;
	    m = d1.month();
	    d = d1.day();
	    y = d1.year();
	    
	    m = months + m;
	    while (m > 12) { // addition of months
	    	y++;
	    	m -=12;
	    }
	    
	    while (m < 1) { // subtraction of months
	    	m += 12;
	    	assert(y!=0);
	    	y--;
	    }

	    // final check - may need to truncate days in month
	    SDate dtemp = new SDate(y,m,1);
	    if (d>dtemp.daysThisMonth()) {
	    	d = dtemp.daysThisMonth();
	    }
	    dtemp.setDay(d);
	    return dtemp;
	}
	
	public SDate addYears(SDate d1, int years) {
	    int d, m,y;
	    d = d1.day();
	    m = d1.month();
	    y = d1.year() + years; 
	    
	    // possibly (iff today is feb 29) then Date(y,m,d) is an invalid date
	    SDate dtemp = new SDate(y,m,1);
	    if (d>dtemp.daysThisMonth()) {
	    	d = dtemp.daysThisMonth();
	    }
	    dtemp.setDay(d);
	    return dtemp;
	}
	
	public SDate addDays(SDate d1, int days) {
		SDate dtemp = new SDate(d1);
	    dtemp.addDays(days);
	    return dtemp;
	}
	
	public SDate nthDayOfMonth(int yyyy, int month, int nth, int dayofweek) {
		// third monday in dec 2006: nthDayOfMonth(2006,12,3,1);	
	    SDate tmp1 = new SDate(yyyy,month,1);
	    // nth = 0 assumed to mean nth=1
	    while (tmp1.dayOfWeek() != dayofweek) {
	    	tmp1.addDays(1); // first occurrence
	    }
	    for (int i = 1;i<nth;i++) {
	    	tmp1.addDays(7);
	    }
	    return tmp1; // note that crap inputs can make tmp1 part of NEXT month	    
	}

	protected SDate rollDate(SDate d, RollConvention conv, HolidayCalendar hc) {		
		SDate d1 = new SDate(d);		  
		// If today is a holiday, do a roll of some kind
		if (conv == RollConvention.PRECEDING) {
			while (!hc.isBusinessDay(d1)) {
				d1.addDays(-1);
			}
			return d1;
		}
		  
		if (conv == RollConvention.FOLLOW) {
			while (!hc.isBusinessDay(d1)) {
				d1.addDays(1);
			}
		    return d1;
		}
		  
		if (conv == RollConvention.MOD_FOLLOW) {
			while (!hc.isBusinessDay(d1)){
				d1.addDays(1);
			}
		    if (d1.month() != d.month()) {
		      d1 = new SDate(d);
		      while(!hc.isBusinessDay(d1)) {
		    	  d1.addDays(-1);
		      }
		    }
		    return d1;
		}
		
		if (conv == RollConvention.MOD_PRECEDING) {
			while (!hc.isBusinessDay(d1)) {
				d1.addDays(-1);
			}
		    if (d1.month() != d.month()) {
		      d1 = new SDate(d);
		      while(!hc.isBusinessDay(d1)) {
		    	  d1.addDays(1);
		      }
		    }
		    return d1;
		}
		
		if (conv == RollConvention.EOM) { // last business day of the month
			d1.setDay(d1.daysThisMonth());
			while (!hc.isBusinessDay(d1)) {
				d1.addDays(-1);
			}
		    return d1;
		}
		
		if (conv == RollConvention.EOM_FOLLOW) { // last day of the month, rolled forward if necc
		    d1.setDay(d1.daysThisMonth());
		    while (!hc.isBusinessDay(d1)) {
		    	d1.addDays(1);
		    }
		    return d1;
		}
		 
		if (conv == RollConvention.BOM) { // first business day of the month, forward roll
		    d1.setDay(1);
		    while (!hc.isBusinessDay(d1)) {
		    	d1.addDays(1);
		    }
		    return d1;
		}
		
		return d1;
	}
	
	public SDate nextBusDay(SDate d, HolidayCalendar hc)
	{
		SDate dl = new SDate(d);
		do {
			dl.addDays(1);			
		}  while (!hc.isBusinessDay(dl));
		
		return dl;		
	}
	
	public SDate dateAdd(SDate t, int N, RollConvention rollconv, Frequency freq, HolidayCalendar hc) {
		SDate d = new SDate(t);
		switch(freq) {
		case MONTHLY:
			d = addMonths(t,N);
			break;
		case DAILY:
			d = addDays(t, N);
			break;
		case ANNUAL:
			d = addYears(t, N);
			break;
		case WEEKLY:
			d = addDays(t, N*7);
			break;
		case QUARTERLY:
			d = addMonths(t,N*3);
			break;
		case BIWEEKLY:
			d = addDays(t, N*14);
			break;
		case SEMIANNUAL:
			d = addMonths(t, N*6);
			break;
		case ZERO:
		default:	
		}
		d = rollDate(d, rollconv, hc);
		return d;
	}
	
	public List<SDate> createDates(SDate d, int years, RollConvention conv, Frequency freq, HolidayCalendar hc) {
		List<SDate> lstDate = new ArrayList<SDate>();
		SDate d1 = new SDate(d);
		lstDate.add(d1);		
		
		int count = years;
		switch(freq) {
		case MONTHLY:
			count = years * 12;
			break;
		case DAILY:
			count = years * 252;
			break;
		case ANNUAL:
			count = years;
			break;
		case WEEKLY:
			count = years * 52;
			break;
		case QUARTERLY:
			count = years * 4;
			break;
		case BIWEEKLY:
			count = years * 26;
			break;
		case SEMIANNUAL:
			count = years * 2;
			break;
		case ZERO:
		default:	
		}
		
		for(int i=0; i<count; ++i) {				
			lstDate.add(dateAdd(d, i+1, conv, freq, hc));
		}		  		    

		return lstDate;
	}
}
