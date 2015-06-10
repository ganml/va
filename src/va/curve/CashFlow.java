package va.curve;

import va.calendar.*;

public class CashFlow {
	protected double amount;
	protected SDate date;
	
	public CashFlow(double amount, SDate date) {
		this.amount = amount;
		this.date = date;
	}
	
	public SDate date() {
		return date;
	}
	
	public double amount() {
		return amount;
	}
}
