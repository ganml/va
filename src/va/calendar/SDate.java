package va.calendar;

public class SDate {
	private int y;
	private int m;
	private int d;
	
	public SDate() {
		y=0;
		m=0;
		d=0;
	}
	
	public SDate(SDate sd) {
		y = sd.y;
		m = sd.m;
		d = sd.d;
	}
	
	public SDate(int y, int m, int d) {
		this.y = y;
		this.m = m;
		this.d = d;
	}
	
	public SDate(String yyyyMMdd) {
		setDateyyyymmdd(Integer.parseInt(yyyyMMdd));
	}
	
	public void SetDate(int y, int m, int d) {
		this.y = y;
		this.m = m;
		this.d = d;
	}
	
	public int yyyymmdd() {
	  return y * 10000 + m * 100 + d;
	}
	
	public void setDateyyyymmdd(int l)
	{
		int tmp1  = l % 10000;
		y = l / 10000;
		d = l % 100;
		m = tmp1 / 100;
		
		// possible invalid date by the way
		if (m>12) m=12;
		if (m==0) m=1;
		if (d==0) d=1;
		if (d > daysThisMonth()) d=daysThisMonth();		
	}
	
	public int daysThisMonth() {
	    int[] days =  { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
	    int[] days2 = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
	    return isLeapYear() ? days2[m-1] : days[m-1];
	}
	
	public boolean isLeapYear() {
		return (y % 4 == 0 && (y % 100 != 0 || y % 400 ==0) );
	}

	public void setDateExcel(double excelDaysSince31dec1899) { 
		// Excel maps 61==1-mar-1900, excel maps 60 to 29-feb-1900, a nonexistent date		  
		// Excel has  1 ==  1-jan-1900
		//           59 == 28-feb-1900
		//           60 == 29-feb-1900  * nonexistent date
		//           61 ==  1-mar-1900
		
		// we do not guarantee matching with Excel for inputs
		// less than 61...
		  
		double epoch = (new SDate(1899,12,30)).julianDate();
		fromJulian(epoch + excelDaysSince31dec1899);
	}
	
	public double julianDate() {
		assert(y>=1582);
		long Y = y;
		long M = m;
		if (m <3) {
			M+=12;
		    Y-=1;
		}
		long A = (long) (Y/100);
		long B = (long) (A/4);
		long C = 2-A+B;
		long E = (long) (365.25*(Y+4716));
		long F = (long) (30.6001*(M+1));
		double JD = (double) (C+d+E+F-1524.5);
		return JD;		
	}
	
	public void fromJulian(double jul) {
		  // sets the current date in gregorian calendar based on the julian time

		  double Z =  jul + 0.5;
		  double F = Z - Math.floor(Z);
		  long W = (long) ((Z - 1867216.25)/36524.25);
		  long X = W / 4;
		  long A = (long) Z + 1+ W -X;
		  long B = A+1524;
		  long C = (long) (((double)B-122.1)/365.25);
		  long D = (long) (365.25*C);
		  long E = (long) ((B-D)/30.6001);
		  long F2 = (long) (30.6001*E+F);
		  d = (int) (B-D-F2);
		  if (E>13) {
			  m = (int) (E-13);
		  } else {
			  m = (int) (E-1);
		  }
		  if (m<3) {
			  y = (int) (C - 4715);
		  } else {
			  y = (int) (C - 4716);
		  }
	}
	
	public void setYear(int yi) { 
		y=yi; 
	}
	  
	public void setMonth(int mi) { 
		m = mi; 
	}
	
	public void setDay(int di) { 
		d = di; 
	}

	public int dayOfWeek() { 
		// 0 = sunday
	    // From C FAQ, by Tomohiko Sakamoto
	    int[] t = {0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4};
	    int iy = y;
	    if(m<3) {
	    	iy -= 1;
	    }
	    
	    return (iy + iy/4 - iy/100 + iy/400 + t[m-1] + d) % 7;
	}
	
	public int dayOfYear() {
		// return days since jan 1. Jan 1 returns "0"
		SDate jan1 = new SDate(y,1,1);
		double ddays = julianDate() - jan1.julianDate();
		return (int) ddays;
	}
	
	public boolean isWeekDay() {
	    int d = dayOfWeek();
	    return (d>0 && d<6);
	}
	
	public int day() {
		return d;
	}
	
	public int month() {
		return m;		
	}
	
	public int year() {
		return y;
	}
	
	public double asExcelDate() {
		return julianDate() - (new SDate(1899,12,30)).julianDate();
	}
	
	public int daysThisYear() { 
		if(isLeapYear() ) {
			return 366;
		} else {
			return 365;
		}
	}
	
	public void addDays(int dt)
	{
		// adds a positive or negative number of days to the current date
		double dnew = julianDate() + dt;
		fromJulian(dnew);
	}
	
	public boolean equals(SDate sd) {
		return (d == sd.day() &&
			    m == sd.month() &&
			    y == sd.year());
	}
	
	public String toString() {
	    String[] mStr = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	    return String.format("%02d%s%d", d, mStr[m-1], y);
	}
	
	public double substract(SDate sd) {
		return julianDate() - sd.julianDate();
	}
	
	public boolean before(SDate sd) {
		return sd.after(this);
	}
	
	public boolean after(SDate sd) {
	    if (y  < sd.year()) {
	    	return false;
	    }
	    if (y > sd.year()) {
	    	return true;
	    }
	    // d1.year==d2.year
	    if (m < sd.month()) {
	    	return false;
	    }
	    if (m > sd.month()) {
	    	return true;
	    }
	    // d1.month == d2.month
	    return (d > sd.day());
	}
	
	public int monthsBetween(SDate sd) {		
		return (sd.year()-y)*12 + sd.month() - m;
	}
	
}
