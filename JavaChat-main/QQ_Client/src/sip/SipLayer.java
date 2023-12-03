package sip;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TooManyListenersException;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.swing.JOptionPane;



public class SipLayer implements SipListener {

    private MessageProcessor messageProcessor;

    private String username;

    private SipStack sipStack;

    private SipFactory sipFactory;

    private AddressFactory addressFactory;

    private HeaderFactory headerFactory;

    private MessageFactory messageFactory;

    private SipProvider sipProvider;

    /** ��ʼ��SIP��ջ */
    public SipLayer(String username, String ip, int port)
	    throws PeerUnavailableException, TransportNotSupportedException,
	    InvalidArgumentException, ObjectInUseException,
	    TooManyListenersException {
	setUsername(username);
	sipFactory = SipFactory.getInstance();
	sipFactory.setPathName("gov.nist");
	Properties properties = new Properties();
	properties.setProperty("javax.sip.STACK_NAME", "TextClient");
	properties.setProperty("javax.sip.IP_ADDRESS", ip);

	/*������Ϣ���ļ�*/
	properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
	properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
		"textclient.txt");
	properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
		"textclientdebug.log");

	sipStack = sipFactory.createSipStack(properties);
	headerFactory = sipFactory.createHeaderFactory();
	addressFactory = sipFactory.createAddressFactory();
	messageFactory = sipFactory.createMessageFactory();

	ListeningPoint tcp = sipStack.createListeningPoint(port, "tcp");
//	ListeningPoint udp = sipStack.createListeningPoint(port, "udp");

	sipProvider = sipStack.createSipProvider(tcp);
	sipProvider.addSipListener(this);
//	sipProvider = sipStack.createSipProvider(udp);
//	sipProvider.addSipListener(this);
    }

    /**
     * ʹ��sip��ջ������Ϣ. 
     */
    public void sendMessage(String to, String message) throws ParseException,
	    InvalidArgumentException, SipException {

	SipURI from = addressFactory.createSipURI(getUsername(), getHost()
		+ ":" + getPort());
	Address fromNameAddress = addressFactory.createAddress(from);
	fromNameAddress.setDisplayName(getUsername());
	FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
		"textclientv1.0");

	String username = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
	String address = to.substring(to.indexOf("@") + 1);

	SipURI toAddress = addressFactory.createSipURI(username, address);
	Address toNameAddress = addressFactory.createAddress(toAddress);
	toNameAddress.setDisplayName(username);
	ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

	SipURI requestURI = addressFactory.createSipURI(username, address);
	requestURI.setTransportParam("tcp");

	ArrayList viaHeaders = new ArrayList();
	ViaHeader viaHeader = headerFactory.createViaHeader(getHost(),
		getPort(), "tcp", "branch1");
	viaHeaders.add(viaHeader);

	CallIdHeader callIdHeader = sipProvider.getNewCallId();

	CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1,
		Request.MESSAGE);

	MaxForwardsHeader maxForwards = headerFactory
		.createMaxForwardsHeader(70);

	Request request = messageFactory.createRequest(requestURI,
		Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
		toHeader, viaHeaders, maxForwards);

	SipURI contactURI = addressFactory.createSipURI(getUsername(),
		getHost());
	contactURI.setPort(getPort());
	Address contactAddress = addressFactory.createAddress(contactURI);
	contactAddress.setDisplayName(getUsername());
	ContactHeader contactHeader = headerFactory
		.createContactHeader(contactAddress);
	request.addHeader(contactHeader);

	ContentTypeHeader contentTypeHeader = headerFactory
		.createContentTypeHeader("text", "plain");
	request.setContent(message, contentTypeHeader);

	sipProvider.sendRequest(request);
    }

    

    public void sendFile(String to, String filePath) throws ParseException,
    InvalidArgumentException, SipException, IOException {

    	byte[] fileContent = Files.readAllBytes(Path.of(filePath));
        
        SipURI from = addressFactory.createSipURI(getUsername(), getHost()
                + ":" + getPort());
        Address fromNameAddress = addressFactory.createAddress(from);
        fromNameAddress.setDisplayName(getUsername());
        FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
                "textclientv1.0");
        
        String username = to.substring(to.indexOf(":") + 1, to.indexOf("@"));
        String address = to.substring(to.indexOf("@") + 1);
        
        SipURI toAddress = addressFactory.createSipURI(username, address);
        Address toNameAddress = addressFactory.createAddress(toAddress);
        toNameAddress.setDisplayName(username);
        ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);
        
        SipURI requestURI = addressFactory.createSipURI(username, address);
        requestURI.setTransportParam("tcp");
        
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
        ViaHeader viaHeader = headerFactory.createViaHeader(getHost(),
                getPort(), "tcp", "branch1");
        viaHeaders.add(viaHeader);
        
        CallIdHeader callIdHeader = sipProvider.getNewCallId();
        
        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1,
                Request.MESSAGE);
        
        MaxForwardsHeader maxForwards = headerFactory
                .createMaxForwardsHeader(70);
        
        Request request = messageFactory.createRequest(requestURI,
                Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);
        
        SipURI contactURI = addressFactory.createSipURI(getUsername(),
                getHost());
        contactURI.setPort(getPort());
        Address contactAddress = addressFactory.createAddress(contactURI);
        contactAddress.setDisplayName(getUsername());
        ContactHeader contactHeader = headerFactory
                .createContactHeader(contactAddress);
        request.addHeader(contactHeader);
        
        ContentTypeHeader contentTypeHeader = headerFactory
                .createContentTypeHeader("application", "octet-stream");
        request.setContent(fileContent, contentTypeHeader);
        System.out.println(fileContent);
        sipProvider.sendRequest(request);
    }
 
    

    /** ��Ӧ����ʱ����. */
    public void processResponse(ResponseEvent evt) {
	Response response = evt.getResponse();
	int status = response.getStatusCode();

	if ((status >= 200) && (status < 300)) { //�ɹ�
	    messageProcessor.processInfo("�����ѷ��͡���");
	    return;
	}

	messageProcessor.processError("Previous message not sent: " + status);
    }

    
    
    
    
    
    /** 
     * �������󵽴�ʱ��SIP��ջ�����ô˷�����
     */
    public void processRequest(RequestEvent evt) {
    	Request req = evt.getRequest();

    	String method = req.getMethod();
    	if (!method.equals("MESSAGE")) { //request type.
    	    messageProcessor.processError("Bad request type: " + method);
    	    return;
    	}

    	FromHeader from = (FromHeader) req.getHeader("From");
//    	messageProcessor.processMessage(from.getAddress().toString(),
//    		new String(req.getRawContent()));
    	Response response = null;
    	// ��ȡContent-Typeͷ
    	ContentTypeHeader contentTypeHeader =(ContentTypeHeader) req.getHeader(ContentTypeHeader.NAME);
    	
    	if (contentTypeHeader != null) {
    		// �ж���Ϣ�������
    		String contentType = contentTypeHeader.getContentType();
    		String contentSubType = contentTypeHeader.getContentSubType();

    		if ("text".equalsIgnoreCase(contentType) && "plain".equalsIgnoreCase(contentSubType)) {
    			// �����ı���Ϣ
    			messageProcessor.processMessage(from.getAddress().toString(),
    		    		new String(req.getRawContent()));
    			try { //�ظ�ΪOK
    			    response = messageFactory.createResponse(200, req);
    			    ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
    			    toHeader.setTag("888"); 
    			    ServerTransaction st = sipProvider.getNewServerTransaction(req);
    			    st.sendResponse(response);
    			} catch (Throwable e) {
    			    e.printStackTrace();
    			    messageProcessor.processError("Can't send OK reply.");
    			}
    		} else if ("application".equalsIgnoreCase(contentType) && "octet-stream".equalsIgnoreCase(contentSubType)) {
    			// �����ļ���Ϣ
    			messageProcessor.processMessage(from.getAddress().toString(),
       		    		"ѡ���ļ�·��");
    			try { //Reply with OK
    	    	    response = messageFactory.createResponse(200, req);
    	    	    ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
    	    	    toHeader.setTag("888"); //This is mandatory as per the spec.
    	    	    ServerTransaction st = sipProvider.getNewServerTransaction(req);
    	    	    //--------------------------------------
    	            //��ȡ������
    	    	   byte[] filecontent = req.getRawContent();
    	    	   String srcPath = JOptionPane.showInputDialog(null, 
    	   				"\n�����뱣���ļ���·��:\n\t��ʽ: C:\\xxx.txt", 
    	   				"·��ѡ��", JOptionPane.INFORMATION_MESSAGE);
    	    	   String filePath = srcPath; // �ļ�·��������

    	           writeByteArrayToFile(filecontent, filePath);
    	           messageProcessor.processMessage(from.getAddress().toString(),
       		    		"�ļ��ѽ���");
    	           //�����ļ�
    	    	   //--------------------------------------
    	    	   st.sendResponse(response);
    	    	} catch (Throwable e) {
    	    	    e.printStackTrace();
    	    	    messageProcessor.processError("Can't send OK reply.");
    	    	}
    		} else {
    			// δ֪���͵���Ϣ
    			messageProcessor.processError("Unknown message type: " + contentType + "/" + contentSubType);
    		}
    	} else {
    		// Content-Typeͷδ����
    		messageProcessor.processError("Content-Type header not set.");
    	}
        }
    
    public static void writeByteArrayToFile(byte[] byteArray, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(byteArray);
            System.out.println("Byte array has been written to the file successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing the byte array to the file: " + e.getMessage());
            e.printStackTrace();
        }
    }

	/** 
     * �������󵽴�ʱ��SIP��ջ�����ô˷�����
     */
//    public void processRequest(RequestEvent evt) {
//	Request req = evt.getRequest();
//
//	String method = req.getMethod();
//	if (!method.equals("MESSAGE")) { //������������
//	    messageProcessor.processError("������������: " + method);
//	    return;
//	}
//
//	FromHeader from = (FromHeader) req.getHeader("From");
//	messageProcessor.processMessage(from.getAddress().toString(),
//		new String(req.getRawContent()));
//	Response response = null;
//	try { //�ظ�ΪOK
//	    response = messageFactory.createResponse(200, req);
//	    ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
//	    toHeader.setTag("888"); 
//	    ServerTransaction st = sipProvider.getNewServerTransaction(req);
//	    st.sendResponse(response);
//	} catch (Throwable e) {
//	    e.printStackTrace();
//	    messageProcessor.processError("Can't send OK reply.");
//	}
//    }

    /** 
     *��û�лش�ʱ��SIP��ջ���ô˷���
     *ע�����������Ϣ�Ĵ���ʽ��ͬ��
     */
    public void processTimeout(TimeoutEvent evt) {
	messageProcessor
		.processError("Previous message not sent: " + "timeout");
    }

    /** 
     * �������첽��Ϣ�������ʱ��SIP��ջ���ô˷�����
     */
    public void processIOException(IOExceptionEvent evt) {
	messageProcessor.processError("Previous message not sent: "
		+ "I/O Exception");
    }

    /** 
     * ���Ի�����ʱ��SIP��ջ�����ô˷����� 
     */
    public void processDialogTerminated(DialogTerminatedEvent evt) {
    }

    /** 
     * ���������ʱ��SIP��ջ�����ô˷�����
     */
    public void processTransactionTerminated(TransactionTerminatedEvent evt) {
    }

    public String getHost() {
	int port = sipProvider.getListeningPoint().getPort();
	String host = sipStack.getIPAddress();
	return host;
    }

    public int getPort() {
	int port = sipProvider.getListeningPoint().getPort();
	return port;
    }

    public String getUsername() {
	return username;
    }

    public void setUsername(String newUsername) {
	username = newUsername;
    }

    public MessageProcessor getMessageProcessor() {
	return messageProcessor;
    }

    public void setMessageProcessor(MessageProcessor newMessageProcessor) {
	messageProcessor = newMessageProcessor;
    }

}
