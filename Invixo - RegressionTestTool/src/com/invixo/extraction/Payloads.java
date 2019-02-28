package com.invixo.extraction;

import java.util.ArrayList;

import com.invixo.common.Payload;

public class Payloads {
	private Payload firstPayload = null;
	private ArrayList<Payload> lastPayloadList = new ArrayList<Payload>();
	
	
	public Payload getFirstPayload() {
		return firstPayload;
	}
	public void setFirstPayload(Payload firstPayload) {
		this.firstPayload = firstPayload;
	}
	
	public ArrayList<Payload> getLastPayloadList() {
		return lastPayloadList;
	}
	public void setLastPayloadList(ArrayList<Payload> lastPayloadList) {
		this.lastPayloadList = lastPayloadList;
	}
	
}
