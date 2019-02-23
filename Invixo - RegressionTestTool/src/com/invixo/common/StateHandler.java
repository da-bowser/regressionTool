package com.invixo.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.invixo.common.util.Logger;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class StateHandler {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = StateHandler.class.getName();
	
	public static final Path FILE_PATH = Paths.get(FileStructure.FILE_STATE);
	
	
	public static String createEntry(String icoName, Payload first, Payload last) {
		final String separator = GlobalParameters.FILE_DELIMITER;
		String line	= System.currentTimeMillis() 
					+ separator 
					
					// FIRST payload
					+ first.getSapMessageKey()
					+ separator 
					+ first.getSapMessageId()
					+ separator 
					+ first.getFileName()
					+ separator
					
					// LAST payload
					+ last.getSapMessageKey()
					+ separator 
					+ last.getSapMessageId()
					+ separator
					+ last.getFileName()
					+ separator
					
					// ICO identifier
					+ icoName;
		
		return line;
	}
	
	
	public static void writeEntry(String stateEntry) throws GeneralException {
		final String SIGNATURE = "writeEntry(String)";
		try {
			if (!Files.exists(FILE_PATH)) {
				Files.createFile(FILE_PATH);
			}
			
			Files.writeString(FILE_PATH, stateEntry, StandardOpenOption.APPEND);
			Files.writeString(FILE_PATH, "\n", StandardOpenOption.APPEND);			
		} catch(IOException e) {
			String msg = "Error writing state entry to file: " + FILE_PATH + ".\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new GeneralException(msg);
		}
	}
	
	
	public static void reset() {
		try {
			Files.deleteIfExists(FILE_PATH);	
		} catch (IOException e) {
			throw new RuntimeException("Error deleting state file: " + FILE_PATH.toString());
		}	
	}
	
}
