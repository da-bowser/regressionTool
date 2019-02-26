package com.invixo.common.util;

import java.util.ArrayList;

public class XiHeader {
	private String messageId = null;
	private String processingMode = null;
	private String qualityOfService = null;
	private ArrayList<XiDynConfRecord> dynamicConfList = new ArrayList<XiDynConfRecord>();
	
	
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getProcessingMode() {
		return processingMode;
	}
	public void setProcessingMode(String processingMode) {
		this.processingMode = processingMode;
	}
	public String getQualityOfService() {
		return qualityOfService;
	}
	public void setQualityOfService(String qualityOfService) {
		this.qualityOfService = qualityOfService;
	}
	public ArrayList<XiDynConfRecord> getDynamicConfList() {
		return dynamicConfList;
	}
	public void setDynamicConfList(ArrayList<XiDynConfRecord> dynamicConfList) {
		this.dynamicConfList = dynamicConfList;
	}
	
}
