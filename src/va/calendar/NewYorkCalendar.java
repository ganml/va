package va.calendar;

public class NewYorkCalendar extends HolidayCalendar {

	@Override
	public boolean isBusinessDay(SDate date) {
	    int w = date.dayOfWeek(); // 0 = sunday .. 6 = saturday
	    int d = date.day(); // 1 .. 31
	    int m = date.month();
	    
	    if ( w == 0 || w == 6) {
	    	return false;
	    }
	    // New year's day
	    if ((d==1 || (d==2 && w==1)) && m==1) { 
	    	return false;	    
	    }	    
	    // Martin Luther King Day
	    if ( (d>=15 && d<=21) && w==1 && m==1) {
	    	return false;
	    }
	    // washington's b'd is 3rd monday in feb
	    if ( (d>=15 && d<=21) && w==1 && m==2) {
	    	return false;
	    }
	    // Last monday in may is memorial day
	    if ( d>= 25 && w == 1 && m == 5) {
	    	return false;
	    }
	    // july 4 || (d==3 && w==5)
	    if ( (d==4 || (d==5 && w==1) ) && m==7) {
	    	return false;
	    }
	    // Labor day first mon in sep
	    if ( d<=7 && w == 1 && m==9) {
	    	return false;
	    }
	    // columbus day, 2nd monday in oct
	    if ( (d>=8 && d<=14) && w==1 && m==10) {
	    	return false;
	    }
	    // veteran's day || (d==10 &&w==5)
	    if ( (d==11 || (d==12 && w==1) ) && m==11) {
	    	return false;
	    }
	    // us thanksgiving, 4th thurs in nov
	    if ( (d>=22 && d<=28) && w==4 && m==11) {
	    	return false;
	    }
	    // xmas || (d==24&&w==5)
	    if ( (d==25 || (d==26&&w==1) ) && m==12) {
	    	return false;
	    }

	    return true;
	}

	public String name() {
		return "New York";
	}
	
}
