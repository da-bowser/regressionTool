package com.invixo.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
	
	private static final String INJECT_FIRST_MSG_ID_TEMPLATE = "<TEMPLATE_INJECT_FIRST_MSG_ID>";
	private static final String NON_INIT_LAST_MSG_ID_TEMPLATE = "<TEMPLATE_NON_INIT_LAST_MSG_ID>";
	private static final String NON_INIT_LAST_MSG_KEY_TEMPLATE = "<TEMPLATE_NON_INIT_LAST_MSG_KEY>";
	private static final String NON_INIT_LAST_FILE_NAME_TEMPLATE = "<TEMPLATE_NON_INIT_LAST_FILE_NAME>";
	private static final String SEPARATOR = GlobalParameters.FILE_DELIMITER;
	
	private static HashMap<String, String> tempMsgLink = new HashMap<String, String>();	// Map of <FIRST msg Id, Inject Id> created during inject.
	private static List<String> icoLines = new ArrayList<String>();		// All lines of an ICO state file
	private static Path icoStatePathSource =  null;			// Path to an ICO state file: Source
	private static Path icoStatePathTarget =  null;			// Path to an ICO state file: Target
	

	public static void init(GlobalParameters.Operation operation, String icoName) {
		final String SIGNATURE = "init(GlobalParameters.Operation, String)";
		switch (operation) {
		case extract : 
			if (Boolean.parseBoolean(GlobalParameters.PARAM_VAL_EXTRACT_MODE_INIT)) {
				icoStatePathSource = Paths.get(FileStructure.DIR_STATE + icoName + "_1_" + operation.toString() + "_init" + ".txt");
				icoStatePathTarget = Paths.get(FileStructure.DIR_STATE + icoName + "_1_" + operation.toString() + "_init" + ".txt");				
			} else {
				icoStatePathSource = Paths.get(FileStructure.DIR_STATE + icoName + "_2_" + GlobalParameters.Operation.inject.toString() + ".txt");
				icoStatePathTarget = Paths.get(FileStructure.DIR_STATE + icoName + "_3_" + operation.toString() + "_nonInit" + ".txt");								
			}
			break;
		case inject : 
			icoStatePathSource = Paths.get(FileStructure.DIR_STATE + icoName + "_1_" + GlobalParameters.Operation.extract.toString() + "_init" + ".txt");
			icoStatePathTarget = Paths.get(FileStructure.DIR_STATE + icoName + "_2_" + operation.toString() + ".txt");
			break;
		case compare : 
			icoStatePathSource = Paths.get(FileStructure.DIR_STATE + icoName + "_3_" + GlobalParameters.Operation.extract.toString() + "_nonInit" + ".txt");
			icoStatePathTarget = null;
			break;
		default :
			throw new RuntimeException("Unsuppported sthdtyjdytjdtyj");
		}
		logger.writeDebug(LOCATION, SIGNATURE,  "State source file: " + icoStatePathSource);
		logger.writeDebug(LOCATION, SIGNATURE,  "State target file: " + icoStatePathTarget);
		icoLines.clear();
	}
	

	public static void storeIcoState() throws StateException {
		final String SIGNATURE = "storeIcoState()";
		try {
			// Delete existing state file
			reset();
			
			// Create file writer
			BufferedWriter bw = Files.newBufferedWriter(icoStatePathTarget);

			// Write header line to file
			final String headerLine	= "TimeInMillis"
					+ SEPARATOR
					+ "InitExtractFirst_MsgKey"
					+ SEPARATOR
					+ "InitExtractFirst_MsgId"
					+ SEPARATOR
					+ "InitExtractFirst_FileName"
					+ SEPARATOR
					+ "InitExtractLast_MsgKey"
					+ SEPARATOR
					+ "InitExtractLast_MsgId"
					+ SEPARATOR
					+ "InitExtractLast_FileName"
					+ SEPARATOR
					+ "inject_MsgId"
					+ SEPARATOR
					+ "NonInitExtractLast_MsgKey"
					+ SEPARATOR
					+ "NonInitExtractLast_MsgId"
					+ SEPARATOR
					+ "NonInitExtractLast_FileName"
					+ SEPARATOR
					+ "IcoName";
			bw.write(headerLine);
			bw.newLine();

			// Write lines to file
			for (String line : icoLines) {
				bw.write(line);
				bw.newLine();
			}

			// Cleanup
			bw.flush();
			bw.close();	

			logger.writeInfo(LOCATION, SIGNATURE, "ICO State persisted to file: " + icoStatePathTarget);

		} catch (IOException e) {
			String msg = "Error updating state file: " + icoStatePathTarget + ".\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new StateException(msg);
		}
	}
	
	
	/**
	 * Read all ICO State lines from File.
	 * @return
	 * @throws StateException
	 */
	private static List<String> readIcoStateLinesFromFile() throws StateException {
		final String SIGNATURE = "readIcoStateLinesFromFile()";
		try {
			if (icoLines.size() == 0) {
				icoLines = Files.readAllLines(icoStatePathSource);
				icoLines.remove(0); // remove header line
			}
			return icoLines;
		} catch (IOException e) {
			String msg = "Error reading ICO lines from state file: " + icoStatePathSource.toString() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new StateException(msg);
		}
	}
	
	
	private static String createEntry(	String icoName, 
										Payload first, 
										Payload last, 
										String injectFirstMsgId, 
										String nonInitLastMsgKey, 
										String nonInitLastMsgId, 
										String nonInitLastFileName) {
		String line	= System.currentTimeMillis() 
					+ SEPARATOR 
					
					// INIT FIRST payload
					+ first.getSapMessageKey()
					+ SEPARATOR 
					+ first.getSapMessageId()
					+ SEPARATOR 
					+ first.getFileName()
					+ SEPARATOR
					
					// INIT LAST payload
					+ last.getSapMessageKey()
					+ SEPARATOR 
					+ last.getSapMessageId()
					+ SEPARATOR
					+ last.getFileName()
					+ SEPARATOR
					
					// INJECT Message Id
					+ injectFirstMsgId
					+ SEPARATOR

					// NON-INIT LAST payload
					+ nonInitLastMsgKey
					+ SEPARATOR 
					+ nonInitLastMsgId
					+ SEPARATOR
					+ nonInitLastFileName
					+ SEPARATOR
					
					// ICO identifier
					+ icoName;
		
		return line;
	}

	
	/**
	 * Scenario: Extract
	 * @param stateEntry
	 */
	public static void addEntryToInternalList(String stateEntry) {
		if (icoLines == null) {
			icoLines = new ArrayList<String>();
		}
		icoLines.add(stateEntry);
	}
		
	
	public static void reset() throws StateException {
		final String SIGNATURE = "reset()";
		try {
			Files.deleteIfExists(icoStatePathTarget);	
		} catch (IOException e) {
			String msg = "Error deleting state file: " + icoStatePathTarget.toString() + "\n" + e;
			logger.writeError(LOCATION, SIGNATURE, msg);
			throw new StateException(msg);
		}	
	}
	
	
	/**
	 * Get list of unique FIRST file names from a list of State lines.
	 * @return
 	 * @throws StateException 
	 */
	public static HashSet<String> getUniqueFirstFileNames() throws StateException {
		readIcoStateLinesFromFile();
		
		HashSet<String> uniqueFirstIds = new HashSet<String>();
		for (String line : icoLines) {
			String messageId = line.split(SEPARATOR)[3];		// File name for a source/original FIRST message Id
			uniqueFirstIds.add(messageId);
		}
		
		return uniqueFirstIds;
	}
	
		
	/**
	 * Scenario: Inject
	 * @param firstMsgId
	 * @param injectMsgId
	 */
	public static void addInjectEntry(String firstMsgId, String injectMsgId) {
		tempMsgLink.put(firstMsgId, injectMsgId);
	}
	
	
	/**
	 * Scenario: Extract NonInit
	 * Create map from ICO State Lines.
	 * @return					Map<key, value>
	 * 								KEY: Source message id (original extracted message id (INIT extract))
	 * 								VAL: Target message id (inject message id)
	 * @throws StateException
	 */
	public static Map<String, String> getMessageIdsFromFile() throws StateException {
		Map<String, String> map = convertLineInfoToMap(2, 7);
		return map;
	}
	
	
	/**
	 * Scenario: Compare
	 * Create map from ICO State Lines.
	 * @return					Map<key, value>
	 * 								KEY: Source message id (init LAST message id)
	 * 								VAL: Target message id (non-init LAST mesage id)
	 * @throws StateException
	 */
	public static Map<String, String> getCompareMessageIdsFromIcoLines() throws StateException {
		Map<String, String> map = convertLineInfoToMap(5, 9);
		return map;
	}
	
	
	private static Map<String, String> convertLineInfoToMap(int keyIndex, int valueIndex) throws StateException {
		// Read lines from file (sets internal property)
		readIcoStateLinesFromFile();
		
		// Create map
		Map<String, String> map = new HashMap<String, String>();
		for (String line : icoLines) {
			String key 		= line.split(SEPARATOR)[keyIndex];
			String value 	= line.split(SEPARATOR)[valueIndex];
			map.put(key, value);
		}
		
		// Return map
		return map;
	}
	

	private static String getSequenceIdFromMessageKey(String messageKey) {
		String[] parts = messageKey.split("\\\\");
		return parts[4];
	}
	
	
	public static String getIcoPath() {
		return icoStatePathSource.toString();
	}


	/**
	 * Scenario: Extract
	 * @param sapMessageId
	 * @param fileName
	 */
	public static void replaceLastFileNameTemplateWithFileName(String sapMessageId, String fileName) {
		for (int i = 0; i < icoLines.size(); i++) {
			String line = icoLines.get(i);
			String currentNonInitLastMessageId = line.split(SEPARATOR)[9];
			
			if (sapMessageId.equals(currentNonInitLastMessageId)) {
				line = line.replace(NON_INIT_LAST_FILE_NAME_TEMPLATE, fileName);
				icoLines.set(i, line);
			}
		}
	}
	
	
	/**
	 * Scenario: Extract NonInit, multimapping
	 * @param injectMessageId
	 * @param nonInitLastMessageKey
	 * @param nonInitLastMessageId
	 * @throws StateException 
	 */
	public static void replaceMessageInfoTemplateWithMessageInfo(String injectMessageId, String nonInitLastMessageKey, String nonInitLastMessageId) throws StateException {
		// Read file
		StateHandler.readIcoStateLinesFromFile();
					
		// Get sequence id from Message Key
		String nonInitLastMessageKeySequenceId = getSequenceIdFromMessageKey(nonInitLastMessageKey);
		
		for (int i = 0; i < icoLines.size(); i++) {
			String line = icoLines.get(i);
			
			// Get parts from current line
			String[] lineParts = line.split(SEPARATOR);
			String currentLastMessageKey = lineParts[4];
			String currentLastKeySequenceId = getSequenceIdFromMessageKey(currentLastMessageKey); 
			String currentInjectMessageId = lineParts[7];

			// Replace templates
			if (nonInitLastMessageKeySequenceId.equals(currentLastKeySequenceId) && injectMessageId.equals(currentInjectMessageId)) {
				String lineWithKey = line.replace(NON_INIT_LAST_MSG_KEY_TEMPLATE, nonInitLastMessageKey);
				String finalLine = lineWithKey.replace(NON_INIT_LAST_MSG_ID_TEMPLATE, nonInitLastMessageId);
				icoLines.set(i, finalLine);
			}
		}
	}
	
	
	/**
	 * Scenario: Inject
	 * Replace INJECT_TEMPLATE with inject Message Id, for all lines containing the referenced 'initFirstMsgId' in internal
	 * map of <initFirstMsgId, injectId>.
	 * Replacement does not store data, it merely updates the internal reference to the State Lines in memory. 
	 */
	public static void replaceInjectTemplateWithId() {
		// Modify internal list of ICO lines
		for (int i = 0; i < icoLines.size(); i++) {
			// Get current line
			String currentLine = icoLines.get(i);
			
			// Split
			String[] lineParts = currentLine.split(SEPARATOR);
			
			// Get FIRST message id
			String currentFirstMsgId = lineParts[2];
			
			// Determine if message id of current line needs to be updated
			boolean isMatchFound = tempMsgLink.containsKey(currentFirstMsgId);
			
			// Replace inject template text with inject id, if the 2 FIRST message ids are the same
			if (isMatchFound) {
				String injectId = tempMsgLink.get(currentFirstMsgId);
				currentLine = currentLine.replace(INJECT_FIRST_MSG_ID_TEMPLATE, injectId);
				icoLines.set(i, currentLine);
			}
		}
	}
	
	
	/**
	 * Scenario: Extract Init
	 * Create an entry.
	 * @param icoName
	 * @param first
	 * @param last
	 * @return
	 */
	public static String createExtractEntry(String icoName, Payload first, Payload last) {
		return createEntry(	icoName, 
							first, 
							last, 
							INJECT_FIRST_MSG_ID_TEMPLATE, 
							NON_INIT_LAST_MSG_KEY_TEMPLATE,
							NON_INIT_LAST_MSG_ID_TEMPLATE,
							NON_INIT_LAST_FILE_NAME_TEMPLATE
							);
	}
	
	
	/**
	 * Scenario: NonInit, Message Split
	 * @param updatedMessageIds		Map of
	 * 									Key: inject id
	 * 									Val: new split message id
	 */
	public static void replaceInjectIdWithSplitId(HashMap<String, String> map) {
		final String SIGNATURE = "replaceInjectIdWithSplitId(HashMap<String, String>)";
		String newSplitId;
		String injectId;
		
		for (HashMap.Entry<String, String> entry : map.entrySet()) {
			injectId = entry.getKey();
			newSplitId = entry.getValue();
			
			for (int i=0; i<icoLines.size(); i++) {
				String currentLine = icoLines.get(i);
				String[] lineParts = currentLine.split(SEPARATOR);
				String currentInjectId = lineParts[7];
				
				// Update entry
				if (injectId.equals(currentInjectId)) {
					logger.writeDebug(LOCATION, SIGNATURE, "Message Split: InjectMsgId '" + injectId + "' replaced with SplitMsgId '" + newSplitId + "'");
					String newLine = currentLine.replace(injectId, newSplitId);
					icoLines.set(i, newLine);
				}
			}
		}
	}
	
}
