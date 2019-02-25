package com.invixo.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.invixo.common.util.Logger;
import com.invixo.consistency.FileStructure;
import com.invixo.main.GlobalParameters;

public class StateHandler {
	private static Logger logger = Logger.getInstance();
	private static final String LOCATION = StateHandler.class.getName();
	
	private static final Path FILE_PATH = Paths.get(FileStructure.FILE_STATE);
	private static final Path FILE_PATH_TEMP = Paths.get(FileStructure.FILE_STATE + "TEMP");
	private static final String INJECT_TEMPLATE = "<TEMPLATE_INJECT_MSG_ID>";
	private static final String SEPARATOR = GlobalParameters.FILE_DELIMITER;
	
	private static HashMap<String, String> tempMsgLink = new HashMap<String, String>();	// Map of <FIRST msg Id, Inject Id> created during inject.

	
	
	/**
	 * Create an entry during init extraction.
	 * @param icoName
	 * @param first
	 * @param last
	 * @return
	 */
	public static String createExtractEntry(String icoName, Payload first, Payload last) {
		return createEntry(icoName, first, last, INJECT_TEMPLATE);
	}
	

	private static String createEntry(String icoName, Payload first, Payload last, String injectMsgId) {
		String line	= System.currentTimeMillis() 
					+ SEPARATOR 
					
					// FIRST payload
					+ first.getSapMessageKey()
					+ SEPARATOR 
					+ first.getSapMessageId()
					+ SEPARATOR 
					+ first.getFileName()
					+ SEPARATOR
					
					// LAST payload
					+ last.getSapMessageKey()
					+ SEPARATOR 
					+ last.getSapMessageId()
					+ SEPARATOR
					+ last.getFileName()
					+ SEPARATOR
					
					// Inject Message Id
					+ injectMsgId
					+ SEPARATOR
					
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
	
	
	/**
	 * Get a list of State lines matching ICO from the State File.
	 * @param icoName
	 * @return
	 */
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

	
	/**
	 * Get list of unique FIRST file names from a list of State lines.
	 * @param lines				Lines to extract unique FIRST IDs from
	 * @return
	 */
	public static HashSet<String> getUniqueFirstFileNames(List<String> lines) {
		HashSet<String> uniqueFirstIds = new HashSet<String>();
		
		for (String line : lines) {
			String messageId = line.split(SEPARATOR)[3];		// File name for a Source/original FIRST message Id
			uniqueFirstIds.add(messageId);
		}
		
		return uniqueFirstIds;
	}
	
	
	/**
	 * Replace INJECT_TEMPLATE with inject Message Id, for all lines containing @param firstMsgId.
	 * This uses internal resource containing Map<FIRST msgId, InjectId> to update all required lines. 
	 * @return
	 */
	public static void replaceInjectTemplateWithId() {
		final String SIGNATURE = "replaceInjectTemplateWithId()";
		try {
			// Read file
			List<String> lines = Files.readAllLines(FILE_PATH);
					
			// Create new list of lines with modified content
			BufferedWriter bw = Files.newBufferedWriter(FILE_PATH_TEMP);
			for (String line : lines) {
				// Split
				String[] lineParts = line.split(SEPARATOR);
				
				// Get FIRST message id
				String currentFirstMsgId = lineParts[2];
				
				// Determine if message id of current line needs to be updated
				boolean isMatchFound = tempMsgLink.containsKey(currentFirstMsgId);
				
				// Replace inject template text with inject id, if the 2 FIRST message ids are the same
				if (isMatchFound) {
					String injectId = tempMsgLink.get(currentFirstMsgId);
					String newLine = line.replace(INJECT_TEMPLATE, injectId);
					bw.write(newLine);
					bw.newLine();
				}
			}
			
			// Cleanup
			bw.flush();
			bw.close();
			
			// Delete original state file
			File file = new File(FILE_PATH_TEMP.toString());
			Files.delete(FILE_PATH);
			
			// Rename temp state file
			boolean isRenamed = file.renameTo(FILE_PATH.toFile());
			if (isRenamed) {
				String msg = "Temp state file is renamed to: " + FILE_PATH.toString();
				logger.writeDebug(LOCATION, SIGNATURE, msg);
			} else {
				String msg = "Temp state file could not be renamed to: " + FILE_PATH.toString();
				logger.writeError(LOCATION, SIGNATURE, msg);
				throw new RuntimeException(msg);
			}
		} catch (IOException e) {
			String msg = "Error reading all ICO lines from state file: " + FILE_PATH.toString() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new RuntimeException(msg);
		}
	}
	
	
	public static void addInjectEntry(String firstMsgId, String injectMsgId) {
		tempMsgLink.put(firstMsgId, injectMsgId);
	}
	
	
	/**
	 * Create MAP from a delimiter separated input file.
	 * @param icoName
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> getMessageIdsFromFile(String icoName) throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		
		// Read file
		List<String> lines = Files.readAllLines(FILE_PATH);
		
		// Filter/remove all lines not containing string
		lines.removeIf(line -> !line.contains(icoName));			
		
		// Create map
		for (String line : lines) {
			String key 		= line.split(SEPARATOR)[2];		// Source message id (original extracted message id (INIT extract))
			String value 	= line.split(SEPARATOR)[7];		// Target message id (inject message id)
			map.put(key, value);
		}
		
		// Return map
		return map;
	}
}
