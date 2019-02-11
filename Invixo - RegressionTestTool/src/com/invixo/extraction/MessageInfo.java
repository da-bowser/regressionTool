package com.invixo.extraction;

import java.util.HashMap;
import java.util.HashSet;

public class MessageInfo {

	private HashSet<String> objectKeys = new HashSet<String>();
	private HashMap<String, String> splitMessageIds = new HashMap<String, String>();
	private int messagesFound = 0;
	
	public HashSet<String> getObjectKeys() {
		return objectKeys;
	}
	public HashMap<String, String> getSplitMessageIds() {
		return splitMessageIds;
	}
	public int getMessagesFound() {
		return messagesFound;
	}
	public void setMessagesFound(int messagesFound) {
		this.messagesFound = messagesFound;
	}
}
