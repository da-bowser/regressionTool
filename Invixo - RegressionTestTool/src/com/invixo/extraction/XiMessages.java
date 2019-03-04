package com.invixo.extraction;

import java.util.ArrayList;

import com.invixo.common.XiMessage;

public class XiMessages {
	private XiMessage firstMessage = null;
	private ArrayList<XiMessage> lastMessageList = new ArrayList<XiMessage>();
	
	
	public XiMessage getFirstMessage() {
		return firstMessage;
	}
	public void setFirstMessage(XiMessage firstMessage) {
		this.firstMessage = firstMessage;
	}
	
	public ArrayList<XiMessage> getLastMessageList() {
		return lastMessageList;
	}
	public void setLastPayloadList(ArrayList<XiMessage> lastMessageList) {
		this.lastMessageList = lastMessageList;
	}
	
}
