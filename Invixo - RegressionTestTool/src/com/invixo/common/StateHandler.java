package com.invixo.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

import com.invixo.common.util.Logger;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class StateHandler {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = StateHandler.class.getName();
	
	private static final Path FILE_PATH = Paths.get(FileStructure.FILE_STATE);
	private static Stream<String> stream = null;

	
	/**
	 * Create an entry during init extraction.
	 * @param icoName
	 * @param first
	 * @param last
	 * @return
	 */
	public static String createExtractEntry(String icoName, Payload first, Payload last) {
		final String injectTemplate = "<TEMPLATE_INJECT_MSG_ID>";
		return createEntry(icoName, first, last, injectTemplate);
	}
	

	private static String createEntry(String icoName, Payload first, Payload last, String injectMsgId) {
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
					
					// Inject Message Id
					+ injectMsgId
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
		final String SIGNATURE = "reset()";
		try {
			Files.deleteIfExists(FILE_PATH);	
		} catch (IOException e) {
			String msg = "Error deleting state file: " + FILE_PATH.toString() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}	
	}
	
	
	public static Stream<String> openStreamToStateFile() {
		final String SIGNATURE = "openStreamToStateFile()";
		try {
			if (stream == null) {
				stream = Files.lines(FILE_PATH);	
			}
			
			return stream;
		} catch (IOException e) {
			String msg = "Error opening stream to state file: " + FILE_PATH.toString() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	
	
	public static void closeStream() {
		if (stream != null) {
			stream.close();	
		}
	}
	
	
	public static List<String> getLinesMatchingIco(String icoName) {
		final String SIGNATURE = "getLinesMatchingIco(String)";
		try {
			// Read file
			List<String> lines = Files.readAllLines(FILE_PATH);
			
			// Filter/remove all lines not having ICO name in it
			lines.removeIf(line -> !line.contains(icoName));			
			
			return lines;			
		} catch (IOException e) {
			String msg = "Error reading all ICO lines from state file: " + FILE_PATH.toString() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}

}
