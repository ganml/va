package va.policy;

import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;

import va.calendar.SDate;
import va.montecarlo.Pricer;
import va.scenario.Scenario;

public class PolicyAger {
	protected Logger log = Logger.getLogger(PolicyAger.class);
	protected Scenario scenario;	
	protected SDate scenarioStartDate;
	public PolicyAger(Scenario scenario, SDate scenarioStartDate) {
		this.scenario = scenario;
		this.scenarioStartDate = scenarioStartDate;
	}
	
	public void agePolicy(Policy p, SDate currentDate) throws Exception {	
		int m = currentDate.year() - p.issueDate.year();
		int j0 = p.issueDate.year() -  scenarioStartDate.year();
		
		Pricer pricer = null;
		String className = "va.montecarlo.Pricer" + p.productType;
		try{
			Constructor<?> cons = Class.forName(className).getConstructor(Scenario.class, int.class); 
			pricer = (Pricer) cons.newInstance(scenario, j0);
		}catch (Exception ex) {
			log.error("Cannot find pricer for " + p.productType);
		}
		
		for(int j=0; j<m; ++j) {
			pricer.project(p, 0, j);
		}
		p.currentDate = currentDate;
		p.age = p.currentDate.year() - p.birthDate.year();
		p.ttm = p.matDate.year() - p.currentDate.year();
		
		for(int i=0; i<p.vFundNum.length; ++i) {
			if(p.vFundValue[i] <0) {
				throw new Exception(String.format("negative fund value %d, %f", p.vFundNum[i], p.vFundValue[i]));	
			}
		}
	}
	
}
