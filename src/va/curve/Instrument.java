package va.curve;

import va.calendar.*;

public abstract class Instrument {
	protected ZeroCurve zc;
	protected DayCountBasis dcb;
	protected SDate startDate;
	protected SDate setDate;
	protected SDate matDate;
	protected HolidayCalendar hc;
	
	public SDate getStartDate() {
		return startDate;
	}
	
	public SDate getSetDate() {
		return setDate;
	}
	
	public SDate getMatDate() {
		return matDate;
	}
	
	public DayCountBasis getDayCountBasis() {
		return dcb;
	}

	public void setZeroCurve(ZeroCurve zc) {
		this.zc = zc;
	}
	
	public HolidayCalendar getHolidayCalendar() {
		return hc;
	}
	
	public abstract double df() throws Exception;
}
