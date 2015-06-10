package va.calendar;

public class CalendarBroker {
	public HolidayCalendar getCalendar(String locale) throws Exception {
		if(locale.toLowerCase().equals("nyb")) {
			return new NewYorkCalendar();
		} else if (locale.toLowerCase().equals("general")) {
			return new HolidayCalendar();
		} else {
			throw new Exception("Cannot find calendar: " + locale);
		}
	}
}
