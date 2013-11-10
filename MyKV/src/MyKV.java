import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.JAXBException;


public class MyKV {

    private static boolean running = true;
    private static Config conf;
    private static MembershipList ownList;
    private static final String configFileName = "mykv.conf";
    private static int contactPort = 0;
    private static String myIP = "";
	
	public static void main(String[] args) throws InterruptedException, SocketException, UnknownHostException, JAXBException, NoSuchAlgorithmException, UnsupportedEncodingException {
	        
	        try {
	            conf = new Config(configFileName);
	        }
	        catch (IOException e) {
	            System.out.println("Failed to load config file: " + configFileName);
	            return;
	        }
	        
	        myIP = conf.valueFor("bindIP");
	        ownList = new MembershipList();
	        
	        String contactIP = conf.valueFor("contactIP");
	        contactPort = conf.intFor("contactPort");
	        
	        int id = (int)(Hash.value(myIP, 6));
	        //System.out.println(String.valueOf(id));
	        
	        if(myIP.equals(contactIP))
	            ownList.add(id, myIP);
	        
	        
	        ConnectionHandler connectionHandler = new ConnectionHandler(conf);
	        Thread handlerThread = new Thread(connectionHandler, "Connection Handler");
	        handlerThread.start();
	        
	        UserinputHandler userinputHandler = new UserinputHandler();
	        Thread userinputThread = new Thread(userinputHandler, "Userinput Handler");
	        userinputThread.start();
	        
	        while(running) {
	            ownList.incrHeartbeatCounter(myIP);
	            KeyValueController kv = new KeyValueController();
	            MembershipController.trackFailing(ownList, conf.intFor("TFail")/1000);
	            MembershipController.sendGossip(ownList, contactIP, contactPort, myIP);
	            
	            //The cleanUp may only be called when the membership list got an update
	            KeyValueController.cleanUp();
	            
	            MembershipList mL = ConnectionHandler.getMembershipList();
	            ownList = mL;
	            Thread.sleep(conf.intFor("TGossip"));
	        }
	                
	        
	        handlerThread.interrupt();
	        userinputThread.interrupt();
	        
	}
	
	
	public static int getContactPort() {
		return contactPort;
	}
	
	public static String getmyIP() {
		return myIP;
	}
	
	public static String getConfigFile() {
		return configFileName;
	}

}