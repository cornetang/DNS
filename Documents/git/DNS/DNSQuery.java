import java.net.InetAddress;
import java.io.*;
import java.net.UnknownHostException;
import java.util.Random;

public class DNSQuery {
	InetAddress server;
	byte[] message;
	String query;

	public DNSQuery (InetAddress rootNameServer, String fqdn)  {
		server = rootNameServer;
		message = encode(fqdn);
		query = fqdn;
	}

	public String getQueryAsString() {
		return query;
	}

	public InetAddress getServerAddress() {
		return server;
	}

	private static byte[] encode(String fqdn) {
		byte[] buf = new byte[18+fqdn.length()];
		
		//random query ID
		Random randomno = new Random();
		byte[] rbyte = new byte[2];
		randomno.nextBytes(rbyte);
		buf[0]=rbyte[0];
		buf[1]=rbyte[1];

		//
		buf[2]=0;buf[3]=0;

		//query count (QDCOUNT)
		buf[4]=0;buf[5]=1;

		//answer count (ANCOUNT)
		buf[6]=0;buf[7]=0;

		//name server record (NSCOUNT)
		buf[8]=0;buf[9]=0;

		//additional record count (ARCOUNT)
		buf[10]=0;buf[11]=0;
		
		//domain name (QNAME)
		int dot_loc = 12; byte label_len = 0; int i;
		for (i=0; i<fqdn.length();i++){
			buf[i+13]=(byte)fqdn.charAt(i);
			if (fqdn.charAt(i)=='.'){
				buf[dot_loc]=label_len;
				label_len=0;
				dot_loc=i+13;
			}else{
				label_len++;
			}
			buf[dot_loc]=label_len;
		
		}

		//end of QNAME
		buf[i+13]=0;

		//QTYPE
		buf[i+14]=0;buf[i+15]=1;

		//QCLASS
		buf[i+16]=0;buf[i+17]=1;
		
		// for debugging only:
		// System.out.println(fqdn.length());
		// System.out.println("address=");
		// for (int l=0;l<fqdn.length()+18;l++){
		// 	System.out.println(l + " " + Integer.toHexString(buf[l] & 0xFF));
		// }
		return buf;
	}

}