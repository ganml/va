package va.main;

import org.apache.commons.cli.*;
import va.montecarlo.PriceHelper;

public class PricerMain {

	public static void main(String[] args) {		
		Options options = new Options();
		
		Option help = new Option( "help", "print this message" );
		options.addOption(help);
		options.addOption("paramfile", true, "file name of the inforce pricer parameter");
		options.addOption("operation", true, "what to do (price,value,createrunfile)");
		
		CommandLineParser parser = new PosixParser();			
		
		try {
			CommandLine cmd = parser.parse( options, args);
			if(cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "InforcePricer", options );
			}
			
			String paramfile = "";
			if(cmd.hasOption("paramfile")) {
				paramfile = cmd.getOptionValue("paramfile");
			} else {
				System.out.println("Please input a paramfile");
				return;
			}
			
			String operation = "value";
			if(cmd.hasOption("operation")) {
				operation = cmd.getOptionValue("operation");
			} 
			
			PriceHelper ph = new PriceHelper(paramfile,operation);
			ph.price();
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}

	}

}
