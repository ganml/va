package va.main;

import org.apache.commons.cli.*;
import va.scenario.GeneratorHelper;

public class ScenarioGeneratorMain {

	public static void main(String[] args) {
		Options options = new Options();
		
		Option help = new Option( "help", "print this message" );
		options.addOption(help);
		options.addOption("paramfile", true, "file name of the parameter file");
		
		CommandLineParser parser = new PosixParser();			
		
		try {
			CommandLine cmd = parser.parse( options, args);
			if(cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "DataGenerator", options );
			}
			
			String paramfile = "";
			if(cmd.hasOption("paramfile")) {
				paramfile = cmd.getOptionValue("paramfile");
			} else {
				System.out.println("Please input a paramfile");
				return;
			}
			
			GeneratorHelper sg = new GeneratorHelper(paramfile);
			sg.generate();
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}

	}

}
