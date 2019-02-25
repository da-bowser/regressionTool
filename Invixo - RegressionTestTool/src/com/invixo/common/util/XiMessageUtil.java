package com.invixo.common.util;

import java.io.IOException;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;


public class XiMessageUtil {

	public static MimeMultipart createMultiPartMessage(byte[] bytes) throws MessagingException {
		// Create multipart message from decoded base64
		DataSource ds = new ByteArrayDataSource(bytes, "multipart/related");
		MimeMultipart mmp = new MimeMultipart(ds);
		return mmp;
	}
	
	
	public static BodyPart getHeaderFromMultiPartMessage(Multipart multiPartMessage) throws MessagingException {
		BodyPart bp = multiPartMessage.getBodyPart(0);		// bodyPart(0) = SAP PO internal envelope (no payload), bodyPart(1) = Payload
		return bp;
	}
	
	
	public static BodyPart getPayloadFromMultiPartMessage(Multipart multiPartMessage) throws MessagingException {
		BodyPart bp = multiPartMessage.getBodyPart(1);		// bodyPart(0) = SAP PO internal envelope (no payload), bodyPart(1) = Payload
		return bp;
	}
	
	
	public static byte[] getPayloadBytesFromMultiPartMessage(Multipart multiPartMessage) throws IOException, MessagingException {
		BodyPart bp = getPayloadFromMultiPartMessage(multiPartMessage);
		return bp.getInputStream().readAllBytes();
	}
	
	
	public static byte[] getPayloadBytesFromMultiPart(byte[] multipartBytes) throws IOException, MessagingException {
			Multipart mmp = XiMessageUtil.createMultiPartMessage(multipartBytes);
			byte[] payload = XiMessageUtil.getPayloadBytesFromMultiPartMessage(mmp);
			return payload;
	}
	
}
