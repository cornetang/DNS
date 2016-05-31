
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.io.*;
import java.net.*;
import java.util.*;
/**
 * 
 */

/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 *
 */
public class DNSlookup {


	static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
	static boolean tracingOn = false;
	static InetAddress rootNameServer;
	static int numberOfQueries = 0;
	static int savedTtl = 0;



	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String fqdn;
		
		int argCount = args.length;
		
		if (argCount < 2 || argCount > 3) {
			usage();
			return;
		}

		rootNameServer = InetAddress.getByName(args[0]);
		fqdn = args[1];
		
		if (argCount == 3 && args[2].equals("-t"))
				tracingOn = true;

		DatagramSocket socket = new DatagramSocket();
		DNSResponse response = lookup(rootNameServer, fqdn, socket);
		InetAddress answer;
		int replyCode;
		int type;
		if (response != null) {
			 answer = response.getIPaddr();
			 replyCode = response.getReplyCode();
			 type = response.getType();
		} else {
			answer = null;
			replyCode = 0;
			type = 1;
		}

		if (answer == null) { // No valid answer
			answer = InetAddress.getByName("0.0.0.0");
		}

		// set ttl
		int ttl;
		if (response == null) { //timeout
			ttl = -2;
		} else if (replyCode == 3) { // Not found
			ttl = -1;
		} else if (replyCode!=0 || (type!=1 && type!=2 && type!=5)) { // Error
			ttl = -4;
		} else if (numberOfQueries >= 30) { // Too many queries
			ttl = -3;
		} else { // Proper TTL
			if (savedTtl != 0) {
				ttl = Math.min(savedTtl, response.getTtl());	
			} else {
				ttl = response.getTtl();	
			}
		} 

    	System.out.println(fqdn + " " + ttl + " " + answer.toString().split("/")[1]);

		socket.close();

	}

	private static DNSResponse lookup(InetAddress ns, String fqdn, DatagramSocket socket) throws Exception {

		//prepare request
    DNSQuery query = new DNSQuery(ns, fqdn);
    byte[] buf = query.message;
    DatagramPacket querypacket = new DatagramPacket(buf, buf.length, ns, 53);
    
    socket.setSoTimeout(5000); // set 5s timeout

    //prepare to receive reply
    byte[] responseBuf = new byte[512];
    DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);

    int tries = 0;
    while (tries < 2) {
    	socket.send(querypacket); // send request
    	tries++;
	    try {
	    	socket.receive(responsePacket); // try to receive reply
	    } catch (SocketTimeoutException e) {
	    	continue; // timeout
	    }
    }
    byte[] received = responsePacket.getData();
    if (received.length == 0) return null;	//timeout
 
    // get response
    DNSResponse response = new DNSResponse(received, received.length, query);	

    // keep track how many queries we make
    numberOfQueries++;
		    
		// print response if -t
    if (tracingOn) response.dumpResponse();
		
		// get info from response
		InetAddress answer = response.getIPaddr();
		String cname = response.getCNAME();
		String nsCname = response.getAuthoritativeDNSname();
		InetAddress nextNs = response.reQueryTo(); 
		int replyCode = response.getReplyCode();

		if (answer != null) { // found the answer!
			// System.out.println("found the answer");
			return response;
		} else if (numberOfQueries>=30) { // too many queries
			// System.out.println("too many queries");
			return response;
		} else if (nextNs != null) { // found another nameserver IP to query
			// System.out.println("found another nameserver IP to query");
			return lookup(nextNs, fqdn, socket);
		} else if (cname != null) { // found a cname to lookup
			 // System.out.println("found a cname to lookup");
			 // System.out.println("savedTtl="+savedTtl+" response.getTtl()="+response.getTtl());
			if (savedTtl == 0) { // save ttl
				savedTtl = response.getTtl();	
			} else {
				savedTtl = Math.min(savedTtl, response.getTtl());
				
			}
			return lookup(rootNameServer, cname, socket);
		} else if (nsCname != null) { // found the cname of a nameserver to look up and use
			 // System.out.println("found the cname of a nameserver to look up and use");
			DNSResponse nsResult = lookup(rootNameServer, nsCname, socket);
			InetAddress newNs = nsResult.getIPaddr();
			if (newNs == null) {
				return response;
			} else {
				return lookup(newNs, fqdn, socket);	
			}
		} else { // error
			// System.out.println("error");
			return response;
		}
		
	}

	private static void usage() {
		System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-t]");
		System.out.println("   where");
		System.out.println("       rootDNS - the IP address (in dotted form) of the root");
		System.out.println("                 DNS server you are to start your search at");
		System.out.println("       name    - fully qualified domain name to lookup");
		System.out.println("       -t      -trace the queries made and responses received");
	}

	
}


