import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;

public class UDPClient extends PingClient{
    /** Host to ping */
    String remoteHost;

    /** Port number of remote host */
    int remotePort;

    /** How many pings to send */
    static final int NUM_PINGS = 10;

    /** How many reply pings have we received */
    int numReplies = 0;

    /** Array for holding replies and RTTs */
    static boolean[] replies = new boolean[NUM_PINGS];
    static long[] rtt = new long[NUM_PINGS];

    /* Send our own pings at least once per second. If no replies received
     within 5 seconds, assume ping was lost. */

    /** 1 second timeout for waiting replies */
    static final int TIMEOUT = 1000;

    /** 5 second timeout for collecting pings at the end */
    static final int REPLY_TIMEOUT = 5000;

    /** constructor **/
    public UDPClient(String host, int port) {
        this.remoteHost = host;
        this.remotePort = port;
    }

    public static void main(String[] args) {
        String host = null;
        int port=0;

        try {
            host = args[0];
            port = Integer.parseInt(args[1]);

        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Need one argument: port number.");
            System.exit(-1);
        } catch (NumberFormatException e) {
            System.out.println("Please give port number as integer.");
            System.exit(-1);
        }
        System.out.println("Contacting host " + host + " at port " + port);

        UDPClient Client = new UDPClient(host, port);
        Client.run();
    }

    public void run() {
        /* Create socket. We do not care which local port we use. */
        PingClient pingClient = new PingClient();
        pingClient.createSocket();

        try {
            pingClient.socket.setSoTimeout(TIMEOUT);

        } catch (SocketException e) {
            System.out.println("Error setting timeout TIMEOUT: " + e);
        }

        for (int i = 0; i < NUM_PINGS; i++) {
            /* Message we want to send to server is just the current time. */
            Date now = new Date();
            String message = "PING " + i + " " + now.getTime() + " ";
            replies[i] = false;
            rtt[i] = 1000000;
            Message ping = null;
            /* Send ping to recipient */
            try {

                ping = new Message(InetAddress.getByName(remoteHost),
                        remotePort, message);

            } catch (UnknownHostException e) {
                System.out.println("Cannot find host: " + e);

            }

            assert ping != null;
            try {
                pingClient.sendPing(ping);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            /* Read the reply by getting the received ping message */
            try {
                Message reply = pingClient.receivePing();
                handleReply(reply.getContents());

            } catch (SocketTimeoutException e) {
                /* Reply did not arrive. Do nothing for now. Figure
                 * out lost pings later. */
            }

        } // end for loop

        try {
            pingClient.socket.setSoTimeout(REPLY_TIMEOUT);

        } catch (SocketException e) {
            System.out.println("Error setting timeout REPLY_TIMEOUT: " + e);
        }

        while (numReplies < NUM_PINGS) {
            try {
                Message reply = pingClient.receivePing();
                handleReply(reply.getContents());

            } catch (SocketTimeoutException e) {
                /* Nothing coming our way apparently. Exit loop. */
                numReplies = NUM_PINGS;
            }
        }

        /* Print statistics */
        for (int i = 0; i < NUM_PINGS; i++) {
            System.out.println("PING " + i + ": " + replies[i]
                    + " RTT: " + rtt[i]);
        }

    } // end run method

    private void handleReply(String contents) {
        String[] tmp = contents.split(" ");

        int pingNumber = Integer.parseInt(tmp[1]);
        long oldtime = Long.parseLong(tmp[2]);
        replies[pingNumber] = true;

        /* Calculate RTT and store it in the rtt-array. */
        numReplies++;

    }
}

