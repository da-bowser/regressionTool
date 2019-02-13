package com.invixo.common.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	
	
	public static String inputstreamToString(InputStream is, String encoding) {
		String result = null;
		try {
			if (is != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				
				while ((length = is.read(buffer)) != -1) {
				    baos.write(buffer, 0, length);
				}
				
				result = baos.toString(encoding);
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException("*inputstreamToString* Error converting stream to string. " + e);
		}
	}
	
	
	public static void writeFileToFileSystem(String filePath, byte[] fileContent) {
		try {
			Path path = Paths.get(filePath);
			Files.write(path, fileContent);			
		} catch (IOException e) {
			throw new RuntimeException("*writeFileToFileSystem* Error writing to file system for file " + filePath + "\n" + e);
		}
	}
	
	
	public static File[] getListOfFilesInDirectory(String directory) {
		File folder = new File(directory);
		
		// DANGER DANGER DANGER - DUMMY IDIOTIC CODE BELOW to make a quick solution ensuring array is ALWAYS initialized and not evar NULL. Sorry!
		File[] files = {};
	
		File[] files2 = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isFile();
			}
		});
		
		if (files2 == null) {
			return files;
		} else {
			return files2;
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
	 * Create MAP from a delimiter separated input file. index parameters determines witch properties to extract into map.
	 * @param path						Path to Message Id mapping file
	 * @param fileDelimiter				Delimiter used in Message Id mapping file
	 * @param filterString				Name of ICO. All ICO names not matching this id are filtered/removed/disregarded
	 * @param keyIndex					Index in map pointing to Source Message Id (the id originally extracted during INIT extract)
	 * @param valueIndex				Index in map pointing to Target Message Id (the id used when injecting)
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> getMessageIdsFromFile(String path, String fileDelimiter, String filterString, int keyIndex, int valueIndex) throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		
		// Read file
		Path mapFilePath = new File(path).toPath();
		List<String> lines = Files.readAllLines(mapFilePath);
		
		// Filter/remove all lines not containing string
		if (filterString != null) {
			lines.removeIf(line -> !line.contains(filterString));			
		}
		
		// Create map
		for (String line : lines) {
			String key 		= line.split(fileDelimiter)[keyIndex];			// Source message id (original extracted message id (INIT extract))
			String value 	= line.split(fileDelimiter)[valueIndex];		// Target message id (inject message id)
			map.put(key, value);
		}
		
		// Return map
		return map;
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

}
