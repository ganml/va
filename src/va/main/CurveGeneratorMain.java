package va.main;

import org.apache.commons.cli.*;
import va.calendar.DateGenerator.Frequency;
import va.curve.*;

public class CurveGeneratorMain {

	public static void main(String[] args) {
		Options options = new Options();
			
		Option help = new Option( "help", "print this message" );
		options.addOption(help);
		options.addOption("datafile", true, "file name of the input curve data");
		options.addOption("resultfile", true, "file name of the output");
		options.addOption("freq", true, "forward rate frequency");
		options.addOption("horizon", true, "forward rate horizon");
			
		
		CommandLineParser parser = new PosixParser();			
		
		try {
			CommandLine cmd = parser.parse( options, args);
			if(cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "DataGenerator", options );
			}
			
			String datafile = "";
			if(cmd.hasOption("datafile")) {
				datafile = cmd.getOptionValue("datafile");
			} else {
				System.out.println("Please input a datafile");
				return;
			}
			
			String resultfile = "CurveOut.csv";
			if(cmd.hasOption("resultfile")) {
				resultfile = cmd.getOptionValue("resultfile");
			} 
			
			String freq = "monthly";
			if(cmd.hasOption("freq")) {
				freq = cmd.getOptionValue("freq").toLowerCase();
			} 
			
			Frequency f = Frequency.MONTHLY;
			if(freq.equals("monthly")) {
				f = Frequency.MONTHLY;
			} else if(freq.equals("annual")) {
				f = Frequency.ANNUAL;
			} else {
				throw new Exception("unrecognized frequency: " + freq);
			}
			
			int horizon = 30;
			if(cmd.hasOption("horizon")) {
				horizon = Integer.parseInt(cmd.getOptionValue("horizon"));
			}
			
			CurveGenerator cg = new CurveGenerator(datafile);
			cg.saveForwardCurve(resultfile, f, horizon);
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
