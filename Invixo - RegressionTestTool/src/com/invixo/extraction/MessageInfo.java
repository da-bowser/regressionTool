package com.invixo.extraction;

import java.util.HashMap;
import java.util.HashSet;

public class MessageInfo {

	private HashSet<String> objectKeys = new HashSet<String>();
	private HashMap<String, String> splitMessageIds = new HashMap<String, String>();
	
	public HashSet<String> getObjectKeys() {
		return objectKeys;
	}
	public HashMap<String, String> getSplitMessageIds() {
		return splitMessageIds;
	}
}
