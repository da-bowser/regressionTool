package com.invixo.extraction;

import java.util.HashMap;
import java.util.HashSet;

class MessageInfo {
	private HashSet<String> objectKeys = new HashSet<String>();
	private HashMap<String, String> splitMessageIds = new HashMap<String, String>();
		
	
	public HashSet<String> getObjectKeys() {
		return objectKeys;
	}
	
	public void setObjectKeys(HashSet<String> objectKeys) {
		this.objectKeys = objectKeys;
	}

	public HashMap<String, String> getSplitMessageIds() {
		return splitMessageIds;
	}

	public void setSplitMessageIds(HashMap<String, String> splitMessageIds) {
		this.splitMessageIds = splitMessageIds;
	}
	
}
