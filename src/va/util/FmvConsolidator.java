package va.util;

import java.io.*;
import java.util.*;

public class FmvConsolidator {
	private String seriatimFile;
	private String aggregateFile;
	private String folder;
	private String newline = System.getProperty("line.separator");
	
	public FmvConsolidator(String folder) {
		this.folder = folder;
	}
	
	public void setSeriatimFile(String seriatimFile) {
		this.seriatimFile = seriatimFile;
	}
	
	public void setAggregateFile(String aggregateFile) {
		this.aggregateFile = aggregateFile;
	}
	
	public void consolidateSeriatim() throws Exception {
		String prefix = seriatimFile.replaceFirst("[.][^.]+$", "");
		File dir = new File(folder);
		File[] files = dir.listFiles();
		List<File> listFile = new ArrayList<File>();
		for (int i = 0; i < files.length; i++) {
		      if (files[i].isFile() && !files[i].getName().equals(seriatimFile) && files[i].getName().startsWith(prefix)) {
		    	  listFile.add(files[i]);
		      }
		}
		
		FileWriter outFile = new FileWriter(seriatimFile);
		PrintWriter out = new PrintWriter(outFile);
		
		int count = 0;
		
		for(File file : listFile) {
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			if(count == 0 ) {
				sb.append(line).append(newline);
			} 
			while(true) {
				line = br.readLine();
				if(line==null  || line.trim().equals("")) {
					break;
				}
				sb.append(line).append(newline);
			}
			++count;
			br.close();
			out.write(sb.toString());				
		}		
		out.close();
				
	}
	
	public void consolidateSeriatimT() throws Exception {
		String prefix = seriatimFile.replaceFirst("[.][^.]+$", "");
		File dir = new File(folder);
		File[] files = dir.listFiles();
		List<File> listFile = new ArrayList<File>();
		for (int i = 0; i < files.length; i++) {
		      if (files[i].isFile() && !files[i].getName().equals(seriatimFile) && files[i].getName().startsWith(prefix)) {
		    	  listFile.add(files[i]);
		      }
		}		
					
		Map<String, Map<String, String>> mapContent = new HashMap<String, Map<String, String>>();
		String[] cell;
		for(File file : listFile) {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			
			while(true) {
				line = br.readLine();
				if(line==null  || line.trim().equals("")) {
					break;
				}
				cell = line.split(",");
				String key = cell[0] + ":" + cell[1];
				if(!mapContent.containsKey(cell[2])) {
					mapContent.put(cell[2], new HashMap<String, String>());
				}
				mapContent.get(cell[2]).put(key, cell[3]);	
			}
			
			br.close();
						
		}				
				
		if(mapContent.size() ==0) {
			return;
		}
		
		FileWriter writer = new FileWriter(seriatimFile);
		List<String> listHeader = new ArrayList<String>();
		
		int count = 0;
		for(String key1 : mapContent.keySet()) {
			if(count==0) {
				for(String key2 : mapContent.get(key1).keySet()) {
					listHeader.add(key2);
				}
				writer.write("RecordID");
				for(int i=0; i<listHeader.size(); ++i) {
					writer.write(",");
					writer.write(listHeader.get(i));
				}
				writer.write(newline);
			} 
			count++;
			writer.write(key1);
					
			for(int i=0; i<listHeader.size(); ++i) {
				writer.write(",");
				writer.write(mapContent.get(key1).get(listHeader.get(i)));
			}
			writer.write(newline);
						
		}
			
		writer.flush();
	    writer.close();
	}
	
	public void consolidateAggregate() throws Exception {
		String prefix = aggregateFile.replaceFirst("[.][^.]+$", "");
		File dir = new File(folder);
		File[] files = dir.listFiles();
		List<File> listFile = new ArrayList<File>();
		for (int i = 0; i < files.length; i++) {
		      if (files[i].isFile() && !files[i].getName().equals(aggregateFile) && files[i].getName().startsWith(prefix)) {
		    	  listFile.add(files[i]);		    	  
		      }
		}
		
		
		StringBuilder sb = new StringBuilder();
		Map<String, Map<String, Double>> mmData = new HashMap<String, Map<String, Double>>();
		int count = 0;		
		String[] cell;
		for(File file : listFile) {			
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			if(count == 0) {
				sb.append(line).append(newline);
			} 
			while(true) {
				line = br.readLine();
				if(line==null || line.trim().equals("")) {
					break;
				}
				cell = line.split(",");
				
				double dTemp = Double.parseDouble(cell[3]);
				if(mmData.containsKey(cell[0])) {
					if(mmData.get(cell[0]).containsKey(cell[1])) {						
						dTemp +=  mmData.get(cell[0]).get(cell[1]);						
						mmData.get(cell[0]).put(cell[1], dTemp);
					} else {
						mmData.get(cell[0]).put(cell[1], dTemp);
					}
				} else {							
					mmData.put(cell[0], new HashMap<String, Double>());
				}
			}
			++count;
			br.close();					
		}		
		
		for(String key1 : mmData.keySet()) {
			for(String key2 : mmData.get(key1).keySet()) {
				sb.append(String.format("%s,%s,0,%f%s", key1, key2, 
						mmData.get(key1).get(key2), newline));
			}
		}
		
		FileWriter outFile = new FileWriter(aggregateFile);
		PrintWriter out = new PrintWriter(outFile);
		out.write(sb.toString());			
		out.close();
				
	}
}
