package com.invixo.extraction;

import java.util.ArrayList;

import com.invixo.common.XiMessage;

public class Payloads {
	private XiMessage firstPayload = null;
	private ArrayList<XiMessage> lastPayloadList = new ArrayList<XiMessage>();
	
	
	public XiMessage getFirstPayload() {
		return firstPayload;
	}
	public void setFirstPayload(XiMessage firstPayload) {
		this.firstPayload = firstPayload;
	}
	
	public ArrayList<XiMessage> getLastPayloadList() {
		return lastPayloadList;
	}
	public void setLastPayloadList(ArrayList<XiMessage> lastPayloadList) {
		this.lastPayloadList = lastPayloadList;
	}
	
}
