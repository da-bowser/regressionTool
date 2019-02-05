package com.invixo.extraction;

import java.util.ArrayList;
import java.util.HashMap;

public class MessageInfo {

	private ArrayList<String> objectKeys = new ArrayList<String>();
	private HashMap<String, String> splitMessageIds = new HashMap<String, String>();
	
	public ArrayList<String> getObjectKeys() {
		return objectKeys;
	}
	public void setObjectKeys(ArrayList<String> objectKeys) {
		this.objectKeys = objectKeys;
	}
	public HashMap<String, String> getSplitMessageIds() {
		return splitMessageIds;
	}
	public void setSplitMessageIds(HashMap<String, String> splitMessageIds) {
		this.splitMessageIds = splitMessageIds;
	}
	
}
