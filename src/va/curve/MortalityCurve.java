package va.curve;

import java.io.*;
import java.util.*;

public class MortalityCurve {
	protected Map<Integer, Double> mapMort;
	protected String fileName;
	protected String assumption;
	
	public MortalityCurve(String fileName, String assumption) throws Exception {
		this.fileName = fileName;
		this.assumption = assumption;
		
		loadMortality();
	}
	
	public MortalityCurve(String fileName) throws Exception {
		this.fileName = fileName;
		this.assumption = "udd";
		
		loadMortality();
	}
	
	protected void loadMortality() throws Exception {
		mapMort = new HashMap<Integer, Double>();
		
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line;
		String[] cell;
        line = br.readLine();
        while (!line.startsWith("Row\\Column"))
        {
            line = br.readLine();
        }
        while (true)
        {
            line = br.readLine();
            if (line == null || line.trim().equals(""))
            {
                break;
            }

            cell = line.split(",");
            if(cell.length !=2) {
            	br.close();
            	throw new Exception("bad mortality line: " + line);
            }
            mapMort.put(Integer.parseInt(cell[0]), Double.parseDouble(cell[1]));
        }

        br.close();


	}
	
	public double p(double x, double t) {
		if(t<=0) {
			return 1.0;
		}
		int x0 = (int) Math.floor(x);
		int t0 = (int) Math.floor(x+t);
		double px0t0 = 1.0;
		for(int i=x0; i<t0; ++i) {
			px0t0 *= 1-mapMort.get(i);
		}
		
		return px0t0 * (1-(x+t-t0)*mapMort.get(t0)) / (1-(x-x0) * mapMort.get(x0));				
		
	}
	
	public double q(double x, double t) {
		return 1-p(x,t);
	}
}
