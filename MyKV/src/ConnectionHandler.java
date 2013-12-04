import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.swing.event.AncestorEvent;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class ConnectionHandler implements Runnable {

    private boolean shouldRun = true;
    private Config conf;
    private int bufferSize = 2048;
    private static MembershipList list = new MembershipList();
    private KeyValueController<String> kvc;
    
    public ConnectionHandler(Config conf, KeyValueController<String> keyValueController) {
    	this.conf = conf;
        kvc = keyValueController;
    }
    
    @Override
    public void run() {
		
        int port = conf.intFor("contactPort");
		
        DatagramSocket rcvSocket = null;
        try {
            rcvSocket = new DatagramSocket(port);
        } catch (SocketException e1) {
            System.out.println("Can't listen on port "+port +"\n");
            return;
        }
        
        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        System.out.println("Waiting for UDP packets: Started");

        while(shouldRun) {
            try {
                rcvSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            
            String msg = new String(buffer, 0, packet.getLength());
            //System.out.println("\nMessage from: " + packet.getAddress().getHostAddress() +", msg: "+msg);
            
            InputSource source = new InputSource(new StringReader(msg));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            Element a = null;
            
            try {
                db = dbf.newDocumentBuilder();
                Document doc = db.parse(source);
                a = doc.getDocumentElement();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
                   
            
           
            
            if(a.getNodeName() == "join") {

                String newMember = packet.getAddress().getHostAddress();
                
    	        int id = 0;
                try {
                    id = (int)(Hash.value(newMember, 6));
                } catch (NoSuchAlgorithmException e1) {
                    e1.printStackTrace();
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
                
                System.out.println(newMember + " is joining the cluster.");
				
                if(!list.ipExists(newMember)) {
					
                    list.add(id, newMember);

                    ArrayList<MembershipEntry> memList = list.get();

                    try {
                        String marshalledMessage = DstrMarshaller.toXML(memList);
                        Supplier.send(newMember, port, marshalledMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JAXBException e) {
                        e.printStackTrace();
                    }
					
                }

                
                
                // Go this way, when the node receives a leave-request from another node
            } else if(a.getNodeName() == "leave") {
                
                // Go this way, when the node gets a membershiplist from another node
            } else if(a.getNodeName() == "membershipList") {
                    
                ArrayList<MembershipEntry> receivedMemList = new ArrayList<MembershipEntry>();
                
                try {
                    receivedMemList = DstrMarshaller.unmarshallXML(msg);
                } catch (JAXBException e) {
                    e.printStackTrace();
                }
                    
                MembershipController.updateMembershipList(list, receivedMemList);
                
            }
            
            
                
            
            if(a.getNodeName() == "insert") {
                	
            	NodeList n = a.getChildNodes();
            	int key = 0;
            	String value = null;
            	
            	for(int i=0;i<n.getLength();i++) {
                    if(n.item(i).getNodeName().equals("key")) {
                        key = Integer.valueOf(n.item(i).getTextContent());
                    }
                    if(n.item(i).getNodeName().equals("value")) {
                        value = n.item(i).getTextContent();
                    }
            	}

            	//Here I assume, that the values are just strings. By calling Str.isInteger / Str.isDouble
            	// it is possible to make a roughly check if the input is an int or double. It would be
            	//possible to save the data in the correct format since generics are used in KVEntry
            	//KeyValueController<String> kvc = new KeyValueController<String>();
            	kvc.insert(key, value, true);
            	
                System.out.println("Key: " + Integer.toString(key) + " | Value: " + value + " inserted.");
            
            } else if(a.getNodeName() == "delete") {
            
            	NodeList n = a.getChildNodes();
            	int key = 0;
            	
            	for(int i=0;i<n.getLength();i++) {
                    if(n.item(i).getNodeName().equals("key")) {
                        key = Integer.valueOf(n.item(i).getTextContent());
                    }
            	}
            	
            	//KeyValueController<String> kvc = new KeyValueController<String>();
            	kvc.delete(key, true);

                System.out.println("Key: " + Integer.toString(key) + " deleted.");

            } else if(a.getNodeName() == "update") {
                
            	NodeList n = a.getChildNodes();
            	int key = 0;
            	String value = null;
            	
            	for(int i=0;i<n.getLength();i++) {
                    if(n.item(i).getNodeName().equals("key")) {
                        key = Integer.valueOf(n.item(i).getTextContent());
                    }
                    if(n.item(i).getNodeName().equals("value")) {
                        value = n.item(i).getTextContent();
                    }
            	}

            	//KeyValueController<String> kvc = new KeyValueController<String>();
            	kvc.update(key, value, true);
            	
                System.out.println("Key: " + Integer.toString(key) + " | Value: " + value + " updated.");

            } else if(a.getNodeName() == "lookup") {
                
            	NodeList n = a.getChildNodes();
            	int key = 0;
            	String type = null;
            	
            	for(int i=0;i<n.getLength();i++) {
                    if(n.item(i).getNodeName().equals("key")) {
                        key = Integer.valueOf(n.item(i).getTextContent());
                    }
                    if(n.item(i).getNodeName().equals("type")) {
                        type = n.item(i).getTextContent();
                    }
            	}

                System.out.println("Key: " + Integer.toString(key) + " lookup.");

            	if(type.equals("send")) {
            		
                    String senderIP = packet.getAddress().getHostAddress();
                    int senderPort = packet.getPort();
            		
                    //KeyValueController<String> kvc = new KeyValueController<String>();
                    kvc.lookup(key, true, senderIP);
	            	
            	} else if(type.equals("receive")) {
                    String value = null;
                    for(int i=0;i<n.getLength();i++) {
                    	if(n.item(i).getNodeName().equals("value")) {
                            value = n.item(i).getTextContent();
                    	}
                    }
                	
                    System.out.println("key: "+String.valueOf(key)+", value: "+value);
            	} else if(type.equals("null")) {
                    System.out.println("Key does not exist");
            	}
            	
            } else if(a.getNodeName() == "show") {
                
            	//KeyValueController<String> kvc = new KeyValueController<String>();
            	ArrayList<KVEntry<String>> al = kvc.showStore();
            	
            	System.out.println("Local Key-Values ------------------------");
            	
            	for(int i=0;i<al.size();i++) {
                    System.out.println("key: "+al.get(i).getKey()+", value: "+al.get(i).getValue());
            	}
            	
            	System.out.println("Membershiplist --------------------------");
            	
            	MembershipList ml = list;
            	ArrayList<MembershipEntry> memList = ml.get();
            	
            	for(int i=0;i<memList.size();i++) {
                    System.out.println("IP: "+memList.get(i).getIPAddress());
            	}
            	
            }
            
            
            
        }
		
    }
	
    public static MembershipList getMembershipList() {
        return list;
    }

    public void kill() {
        this.shouldRun = false;
    }

}
