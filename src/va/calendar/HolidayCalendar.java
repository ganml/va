package va.calendar;

public class HolidayCalendar {
	public boolean isBusinessDay(SDate date) {
	    int w = date.dayOfWeek(); // 0 = sunday .. 6 = saturday
	    
	    if ( w == 0 || w == 6) {
	    	return false;
	    }
	    
	    return true;
	}
	
	public String name() {
		return "WeekendsOnly";
	}
	
	
}
