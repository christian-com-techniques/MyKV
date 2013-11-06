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
	        
	        /*
	        String testip = "192.168.56.102";
	        int idt = (int)(Hash.value(testip, 6));
	        ownList.add(idt, testip);
	        
	        String testip3 = "192.168.56.103";
	        int idt3 = (int)(Hash.value(testip3, 6));
	        ownList.add(idt3, testip3);
	        
	        String testip4 = "192.168.56.104";
	        int idt4 = (int)(Hash.value(testip4, 6));
	        ownList.add(idt4, testip4);
	        
	        String testip5 = "192.168.56.105";
	        int idt5 = (int)(Hash.value(testip5, 6));
	        ownList.add(idt5, testip5);
	        
	        String testip6 = "192.168.56.106";
	        int idt6 = (int)(Hash.value(testip6, 6));
	        ownList.add(idt6, testip6);
	        */
	        
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