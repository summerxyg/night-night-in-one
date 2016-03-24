package com.nightnight.jaxws.client;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class JAXWSClientTest {
	
	public static final String WSDL_URL = "http://uwlmsng.caaccargo.com/lms-ws/WSGateway?wsdl";
	
	public static final String ENDPOINT_ADDRESS = "http://uwlmsng.caaccargo.com/lms-ws/WSGateway";
	
	public static final String NAMESPACE = "http://www.unisys.com/transportation/lms";
	
	public final static QName SERVICE_QNAME = new QName("http://www.unisys.com/transportation/lms", "WSGateway");
	
	public final static QName PORT_QNAME = new QName("http://www.unisys.com/transportation/lms", "WSGatewayPort");
	
	public final static String ACCESS_TOKEN = "VVcjTkdBRE1JTiNhZG1pbm5n";

	@SuppressWarnings({ "unchecked", "restriction" })
	public static <INP, OUT> OUT runSOAPMessageTest(String serviceName, INP payload, Class<OUT> expectClazz) throws Exception{
		// 1. 创建服务
		Service service = Service.create(new URL(WSDL_URL), SERVICE_QNAME);
		// 2. 创建Dispatch
		Dispatch<SOAPMessage> dispatch = service.createDispatch(PORT_QNAME, SOAPMessage.class, Service.Mode.MESSAGE);
		// 3. 设置Endpoint地址
		dispatch.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT_ADDRESS);
		// 4. 一些乱七八糟的设置
		// 4.1 设置HTTP请求头
		Map<String, Object> requestHeaders = new HashMap<String, Object>();
		requestHeaders.put("token", Arrays.asList(ACCESS_TOKEN));
		dispatch.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);
		// 4.2 重新设定Timeout时间?
		dispatch.getRequestContext().put(com.sun.xml.internal.ws.client.BindingProviderProperties.CONNECT_TIMEOUT, 1000);
		dispatch.getRequestContext().put(com.sun.xml.internal.ws.client.BindingProviderProperties.REQUEST_TIMEOUT, 3000);
		
		// 5. 创建SOAPMessage
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage inputMessage = factory.createMessage();
        SOAPPart part = inputMessage.getSOAPPart();
        SOAPEnvelope envelope = part.getEnvelope();
        // 5.1 组装SOAPBody
        SOAPBody body = envelope.getBody();
        // @RequestWrapper
        SOAPElement servElem = body.addBodyElement(new QName(NAMESPACE, "Service", "lms"));
        // 5.2 服务名
        servElem.addChildElement("ServiceName").setValue(serviceName);
        // 5.3 Payload
        SOAPElement payloadElem = servElem.addChildElement("Payload");
        if (payload != null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.newDocument();
    		document.setStrictErrorChecking(false);
    		
    		Class<INP> inputClazz = (Class<INP>) payload.getClass();
    		JAXBContext jaxbContext = JAXBContext.newInstance(inputClazz);
    		Marshaller marshaller = jaxbContext.createMarshaller();
    		if (payload.getClass().getAnnotation(XmlRootElement.class) != null) {
    			marshaller.marshal(payload, document);
    		} else {
    			// Payload 的 QNAME
    			QName payloadQName = new QName("iata:bookingrequest:1", "BookingRequest", "rsm");
    			JAXBElement<INP> input = new JAXBElement<INP>(payloadQName, inputClazz, payload);
    			marshaller.marshal(input, document);
    		}
            SOAPElement soapElem = SOAPFactory.newInstance().createElement(document.getDocumentElement());
            payloadElem.addChildElement(soapElem);
        }
        
        // 6. 调用
        SOAPMessage outputMessage = dispatch.invoke(inputMessage);
        
		JAXBContext jaxbContext = JAXBContext.newInstance(expectClazz);
		Document outputDocument = outputMessage.getSOAPBody().getOwnerDocument();
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		// @ResponseWrapper
		final Node resultNode = outputDocument.getElementsByTagName("Result").item(0);
		final JAXBElement<OUT> jaxbOutputObject = unmarshaller.unmarshal(resultNode, expectClazz);
		return jaxbOutputObject.getValue();
	}
}
