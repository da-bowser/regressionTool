package com.invixo.common.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.invixo.main.GlobalParameters;

public class Util {
	
	public static void main(String[] args) throws Exception {
		// Test: getRelevantMessageIds (empty filter)
		System.out.println("------ Test 1 -------");
		String sourceFilePath = "c:\\Users\\dhek\\Desktop\\TEST\\MapFile.txt";
		Map<String, String> map1 = getMessageIdsFromFile(sourceFilePath, GlobalParameters.FILE_DELIMITER, "", 1, 2);
		for (Entry<String, String> entry : map1.entrySet()) {
			System.out.println(entry.getKey() + " / " + entry.getValue());
		}
		
		// Test: getRelevantMessageIds (non-empty filter)
		System.out.println("\n------ Test 2 -------");
		Map<String, String> map2 = getMessageIdsFromFile(sourceFilePath, GlobalParameters.FILE_DELIMITER, "navn 2", 1, 2);
		for (Entry<String, String> entry : map2.entrySet()) {
			System.out.println(entry.getKey() + " / " + entry.getValue());
		}
	}
	

	public static long getTime() {
		return System.nanoTime();
	}

	
	public static double measureTimeTaken(long start, long end) {
		double milliseconds = (end - start) / 1000000.0;
		double seconds = milliseconds / 1000.0;
		return seconds; 
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
	 * Delete all files in a directory
	 */
	public static void deleteFilesInDirectory(String dir) throws IOException {
		Path directory = Paths.get(dir + ".");
		Stream<Path> fileList = Files.walk(directory);
		Iterator<Path> iter = fileList.iterator();
		
		// Delete every file in directory
		while (iter.hasNext()) {
			Path path = iter.next();
			File file = new File(path.toString());
			
			// Delete files (not directories)
			if (file.isFile()) {
				file.delete();					
			}
		}
		fileList.close();
	}

	
    /**
    * Delete all folders, sub-folders and files.
    * @param dir
    * @throws IOException
    */
    public static void deleteFilesAndSubDirectories(String dir) throws IOException {
    	File file = new File(dir);
    	if (file.exists()) {
    		for (File childFile : file.listFiles()) {
    			if (childFile.isDirectory()) {
    				// Recursive call to delete method to delete any child elements
    				deleteFilesAndSubDirectories(childFile.getAbsolutePath());
    			} else {
                    // Delete child
    				childFile.delete();
    			}
    		}
    		// Delete file or folder
            file.delete();
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
				// Get all files in directory AND subdirectories and return			
				readList = Files.walk(Paths.get(directory)).filter(Files::isRegularFile).collect(Collectors.toList());
			} else {
				// Get all files in directory AND subdirectories and return
				readList = Files.walk(Paths.get(directory)).filter(Files::isDirectory).collect(Collectors.toList());	

				if (readList.size() > 1) {
					// Remove "parent" (self) directory, to only return subdirs
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
	 * @param path
	 * @param fileDelimiter
	 * @param filterString
	 * @param keyIndex
	 * @param valueIndex
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
			String key 		= line.split(fileDelimiter)[keyIndex];
			String value 	= line.split(fileDelimiter)[valueIndex];
			map.put(key, value);
		}
		
		// Return map
		return map;
	}

}
