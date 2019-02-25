package com.invixo.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
	
	public static long getTime() {
		return System.nanoTime();
	}

	
	public static double measureTimeTaken(long start, long end) {
		double milliseconds = (end - start) / 1000000.0;
		double seconds = milliseconds / 1000.0;
		return seconds; 
	}
	
	
	public static String convertBytesToMegaBytes(int inputBytes) {
		DecimalFormat df = new DecimalFormat("#.######");
		double bytes = inputBytes;
		double kb = bytes / 1024;
		double mb = kb / 1024;
		return df.format(mb);
	}
	
		
	public static void writeFileToFileSystem(String filePath, byte[] fileContent) {
		try {
			Path path = Paths.get(filePath);
			Files.write(path, fileContent);			
		} catch (IOException e) {
			throw new RuntimeException("*writeFileToFileSystem* Error writing to file system for file " + filePath + "\n" + e);
		}
	}
	
	
	public static byte[] readFile(String file) {
		try {
			Path path = Paths.get(file);
			byte[] content = Files.readAllBytes(path);
			return content;
		} catch (IOException e) {
			throw new RuntimeException("*readFile* Error reading file: " + file + "\n" + e);
		}
	}
	
	
	/**
	 * Extract the file name from a path.
	 * @param fileName					Path including a file name
	 * @param includeFileExtension		Indicates if file extension should be preserved in output or not
	 * @return
	 */
	public static String getFileName(String fileName, boolean includeFileExtension) {
		File file = new File(fileName);
		if (includeFileExtension) {
			return file.getName();
		} else {
			int index = file.getName().lastIndexOf(".");
			if (index == -1) {
				// There is no extension
				return file.getName();
			} else {
				// There is an extension
				return file.getName().substring(0, index);	
			}
		}	
	}
   
    
    /**
     * Get FILE|DIRECTORY list
     * @param directory		Source destination
     * @param readMode		Type of read to perform FILE or DIRECTORY
     * @return	List<Path>
     */
	public static List<Path> generateListOfPaths(String directory, String readMode) {		
		List<Path> readList = new ArrayList<Path>();
		try {
			if (readMode.equals("FILE")) {
				// Get all files in directory AND sub-directories and return			
				readList = Files.walk(Paths.get(directory)).filter(Files::isRegularFile).collect(Collectors.toList());
			} else {
				// Get all files in directory AND sub-directories and return
				readList = Files.walk(Paths.get(directory)).filter(Files::isDirectory).collect(Collectors.toList());	

				if (readList.size() > 1) {
					// Remove "parent" (self) directory, to only return sub-directories
					readList.remove(0);
				}
			}

		} catch (IOException e) {
			String msg = "Error reading " + readMode + " data from: " + directory + e.getMessage();
			throw new RuntimeException(msg);
		}
		
		// Return list of FILE|DIRECTORIES found
		return readList;
	}
	
	
	/**
	 * Create directories part of a directory path, if they are missing
	 * @param directoryPath
	 */
	public static void createDirIfNotExists(String directorypath) {
		try {
			Files.createDirectories(Paths.get(directorypath));			
		} catch (IOException e) {
			throw new RuntimeException("*createDirIfNotExists* Error creating directories for path: " + directorypath);
		}
	}

	
	/**
	 * Extract SAP Message Id from Message Key.
	 * Example: a3386b2a-1383-11e9-a723-000000554e16\OUTBOUND\5590550\EO\0
	 * @param key
	 * @return
	 */
	public static String extractMessageIdFromKey(String key) {
		String messageId = key.substring(0, key.indexOf("\\"));
		return messageId;
	}
	
}
