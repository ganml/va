package va.main;

import org.apache.commons.cli.*;
import va.util.FmvConsolidator;

public class ConsolidatorMain {

	public static void main(String[] args) {
		Options options = new Options();
		
		Option help = new Option( "help", "print this message" );
		options.addOption(help);
		options.addOption("file", true, "file name of the file to be consolidated");
		options.addOption("folder", true, "folder of the files");
		options.addOption("type", true, "type of consolidation (aggregate, seriatim, seriatimT)");
		
		CommandLineParser parser = new PosixParser();			
		
		try {
			CommandLine cmd = parser.parse( options, args);
			if(cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "InforcePricer", options );
			}
			
			String file = "";
			if(cmd.hasOption("file")) {
				file = cmd.getOptionValue("file");
			} else {
				System.out.println("Please input a file");
				return;
			}
			
			String folder = System.getProperty("user.dir");
			if(cmd.hasOption("folder")) {
				folder = cmd.getOptionValue("folder");
			} 
			
			String type = "aggregate";
			if(cmd.hasOption("type")) {
				type = cmd.getOptionValue("type").toLowerCase();
			} 
			
			FmvConsolidator fc = new FmvConsolidator(folder);
			if(type.equals("aggregate")) {
				fc.setAggregateFile(file);
				fc.consolidateAggregate();
			} else if(type.equals("seriatim") ){
				fc.setSeriatimFile(file);
				fc.consolidateSeriatim();
			} else {
				fc.setSeriatimFile(file);
				fc.consolidateSeriatimT();
			}
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}


	}

}
