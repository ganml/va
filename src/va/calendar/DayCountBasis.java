package va.calendar;

public class DayCountBasis {
	public enum BasisType {
		ACT365, 
		ACT360, 
		ACTACT, 
		THIRTY360
	};
	
	private BasisType _bt;
		
	public DayCountBasis(BasisType t) {
		_bt = t;
	}
	
	public DayCountBasis(String s) {
		if (s==null) {
			_bt = BasisType.ACT365;
			return;
		}
		
		String k = s.toUpperCase();
		if (k=="ACT/365"||k=="ACT365") {
			_bt = BasisType.ACT365;
			return;
		}
			  
		if (k=="ACT/360"||k=="ACT360") {
			_bt = BasisType.ACT360;
			return;
		}
		
		if (k=="30/360"||k=="30360") {
			_bt = BasisType.THIRTY360;
			return;
		}
			 
		if (k=="ACT/ACT"||k=="ACTACT") {
			_bt = BasisType.ACT360;
			return;
		}
			  
		_bt = BasisType.ACT365;
	}
	
	public BasisType getBasisType() {
		return _bt;
	}
	
	public void setType(BasisType b) {
		_bt = b;
	}
	
	public double tfrac(SDate t1, SDate t2) {
		double DAYS = t2.substract(t1);
		switch (_bt) {
		case ACT365:
			return DAYS / 365.;
		case ACT360:
			return DAYS / 360.;
		case THIRTY360:
			return sia_days_30_360(t1,t2) / 360.0;
	    case ACTACT:
		    return julian_act_act_yearfrac(t1,t2);
		default:
		    return DAYS / 365.; 
		}
	}
		
	protected double julian_act_act_yearfrac(SDate t1, SDate t2) {
		if (t2.before(t1)) {
			return -julian_act_act_yearfrac(t2,t1);
		}
		
		int y1 = t1.year();
		int y2 = t2.year();
		
		if (y1 == y2) {
			return t2.substract(t1) / t1.daysThisYear();
		}

		double totdays = 0;
		int yearcount = 0;  
		SDate d1 = new SDate(t1.year(),1,1);
		while (d1.year() < t2.year()) {
			totdays += d1.daysThisYear();
			yearcount ++;
			d1.setYear(d1.year()+1);
		}
		totdays /= yearcount;
		return t2.substract(t1) / totdays;
	}
	
	protected double sia_days_30_360(SDate t1, SDate t2) {
		  if (t2.before(t1) ) {
			  return -sia_days_30_360(t2,t1);
		  }
		  
		  int y1,y2,m1,m2,d1,d2;
		  y1 = t1.year(); m1 = t1.month(); d1 = t1.day();
		  y2 = t2.year(); m2 = t2.month(); d2 = t2.day();

		  if(d1==31) {
			  d1 = 30;
		  }
		  if(d2==31 && (d1==30 || d1==31)) {
			  d2 = 30;
		  }

		  int NOD = (y2-y1)*360 + (m2-m1)*30 + d2-d1;
		  return NOD;
	}
}
