package va.montecarlo;

import va.policy.Policy;
import va.scenario.Scenario;

public class PricerWB extends Pricer {

	public PricerWB(Scenario scenario, int scenarioOffset) {
		super(scenario, scenarioOffset);
		this.name = "WB Pricer";
	}

	@Override
	public void project(Policy p, int sceInd, int timeInd) {
		int T = p.matDate.year() - p.currentDate.year();		
		
		if(T<0) { // maturity
			AV[sceInd][timeInd] = 0.0;
			DB[sceInd][timeInd] = 0.0;
			WB[sceInd][timeInd] = 0.0;
			MB[sceInd][timeInd] = 0.0;
			RC[sceInd][timeInd] = 0.0;
			
			return;
		}
		
		int numFund = p.vFundNum.length;
		double dAV = 0.0;
		double dFee = 0.0;
		for(int k=0; k<numFund; ++k) {					
			double dPartialAV = p.vFundValue[k] *  
					scenario.getFundScenario(p.vFundNum[k])[sceInd][timeInd+scenarioOffset] *
			        (1 - p.vFundFee[k]);
			double dPartialFee = dPartialAV * p.riderFee;
			double dBaseFee = dPartialAV * p.baseFee;
			p.vFundValue[k] = dPartialAV - dPartialFee - dBaseFee;			
			dAV += p.vFundValue[k] ;		
			dFee += dPartialFee;				
		}
		
		// update the policy information
		double dWithdrawal = 0.0;
		double dWB = 0.0;
		int year = p.currentDate.year();
		p.currentDate.setYear(year+1);
		dWithdrawal = Math.min(p.gmwbAmt, p.gmwbBalance);
		p.gmwbBalance -= dWithdrawal;
		p.withdrawal += dWithdrawal;
		
		dWB = Math.max(0.0, dWithdrawal - dAV);
		dAV = Math.max(0.0, dAV - dWithdrawal);
		if(dAV <= 1e-4) {
			for(int k=0; k<numFund; ++k) {		
				p.vFundValue[k] = 0.0;
			}
		} else {
			for(int k=0; k<numFund; ++k) {		
				p.vFundValue[k] *= dAV / (dAV+dWithdrawal);
			}
		}			
		
		
		if(T==0) {
			AV[sceInd][timeInd] = dAV;
			DB[sceInd][timeInd] = Math.max(0, p.gmdbAmt-dAV);
			WB[sceInd][timeInd] = Math.max(0.0, p.gmwbBalance - dAV) + dWB;
			MB[sceInd][timeInd] = 0.0;
			RC[sceInd][timeInd] = dFee;	
		} else {
			AV[sceInd][timeInd] = dAV;
			DB[sceInd][timeInd] = Math.max(0, p.gmdbAmt-dAV);
			WB[sceInd][timeInd] = dWB;
			MB[sceInd][timeInd] = 0.0;
			RC[sceInd][timeInd] = dFee;	
		}

	}

}
