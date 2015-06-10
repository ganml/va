package va.main;

import org.apache.commons.cli.*;

import va.policy.PolicyGenerator;

public class PolicyGeneratorMain {

	public static void main(String[] args) {
		Options options = new Options();
		
		Option help = new Option( "help", "print this message" );
		options.addOption(help);
		options.addOption("paramfile", true, "file name of the inforce generator parameter");
		
		CommandLineParser parser = new PosixParser();			
		
		try {
			CommandLine cmd = parser.parse( options, args);
			if(cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "PolicyGenerator", options );
			}
			
			String paramfile = "";
			if(cmd.hasOption("paramfile")) {
				paramfile = cmd.getOptionValue("paramfile");
			} else {
				System.out.println("Please input a paramfile");
				return;
			}
			
			
			PolicyGenerator cg = new PolicyGenerator(paramfile);
			cg.generate();
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}

	}

}
