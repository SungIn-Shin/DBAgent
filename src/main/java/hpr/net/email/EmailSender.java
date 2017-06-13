// package hpr.net.email;

// import java.io.UnsupportedEncodingException;

// import javax.activation.DataHandler;
// import javax.activation.DataSource;
// import javax.activation.FileDataSource;
// import javax.mail.Authenticator;
// import javax.mail.Message;
// import javax.mail.MessagingException;
// import javax.mail.Multipart;
// import javax.mail.PasswordAuthentication;
// import javax.mail.Session;
// import javax.mail.Transport;
// import javax.mail.internet.InternetAddress;
// import javax.mail.internet.MimeBodyPart;
// import javax.mail.internet.MimeMessage;
// import javax.mail.internet.MimeMultipart;


// public class EmailSender {

// 	public class SMTPAuthenticator extends Authenticator {
// 		PasswordAuthentication passwordAuthentication;
		
// 		SMTPAuthenticator(String userName, String password) {
// 			super();
// 			passwordAuthentication = new PasswordAuthentication(userName, password);
// 		}
// 		public PasswordAuthentication getPasswordAuthentication() {
// 			return passwordAuthentication;
// 		}
// 	};
    
// 	private final String 	smtpHost_;
// 	private final int 		smtpPort_;
	
// 	private final String 	smtpId_;
// 	private final String 	smtpPwd_;

// 	public EmailSender (String smtpHost, int smtpPort, String smtpId, String smtpPwd)  {
// 		smtpHost_ = smtpHost;
// 		smtpPort_ = smtpPort;
		
// 		smtpId_	 = smtpId;
// 		smtpPwd_ = smtpPwd;
// 	}

	
// 	public void send( String senderName, String receivers, String title, String body) throws UnsupportedEncodingException, MessagingException {
// 		sendEmail(getSession(), smtpId_, senderName, receivers, "", "", title, body, null, null);
// 	}
	
// 	public Session getSession() {
// 		return getSession(smtpHost_, smtpPort_, smtpId_, smtpPwd_);
// 	}
	
// 	public Session getSession( String host, int port, String id, String pwd ) {

// 		// Set up the SMTP server.
// 		java.util.Properties props = new java.util.Properties();
// 		props.put("mail.transport.protocol", "smtp");
// 		props.put("mail.smtp.host", host);
// 		props.put("mail.smtp.port", port);
// 		props.put("mail.smtp.auth", "true");
// 		props.put("mail.smtp.starttls.enable", "true");

// 		Authenticator authenticator = new SMTPAuthenticator( id, pwd );
		
// 		return Session.getInstance(props, authenticator);
// 	}
	
// 	public void sendEmail	( Session session
// 							, String fromEmail, String fromName
// 							, String toEmails
// 							, String bccEmail, String bccName
// 							, String subject, String body
// 							, String attachFile
// 							, String attachDisplayFileName ) throws UnsupportedEncodingException
// 																	, MessagingException {
	
// 		Message msg = new MimeMessage(session);

// 		msg.setFrom(new InternetAddress(fromEmail, fromName));
		
		
// 		for( String toEmail: toEmails.split(";")) {
// 			msg.addRecipient(Message.RecipientType.TO, new InternetAddress( toEmail ));
// 		}
		
// 		if( null != bccEmail && !bccEmail.isEmpty()) {
// 			msg.setRecipient(Message.RecipientType.BCC, new InternetAddress( bccEmail, bccName));
// 		}
// 		msg.setSubject(subject);
// 		//msg.setContent(body, "text/html; charset=utf-8");

//         Multipart multipart = new MimeMultipart();
// 		if( null != body && !body.isEmpty()) {
// 			// create the message part 
// 	        MimeBodyPart bodyPartForText = new MimeBodyPart();
// 	        //bodyPartForText.setContent(body, "text/html; charset=euc-kr");
// 	        bodyPartForText.setText(body);
	
// 	        multipart.addBodyPart(bodyPartForText);
// 		}
		
// 		if( null != attachFile && !attachFile.isEmpty()) {
// 	        // Part two is attachment
// 	        MimeBodyPart bodyPartForFile = new MimeBodyPart();
// 	        DataSource source = new FileDataSource(attachFile);
	        
// 	        bodyPartForFile.setDataHandler(new DataHandler(source));
// 	        bodyPartForFile.setFileName(attachDisplayFileName);
// 	        multipart.addBodyPart(bodyPartForFile);
// 		}
		
//         // Put parts in message
//         msg.setContent(multipart);
		
// 		Transport.send(msg);
// 	}
	
// 	public static void main(String[] args) {
// 		// TODO Auto-generated method stub

// 	}

// }
