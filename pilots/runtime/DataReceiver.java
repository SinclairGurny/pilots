package pilots.runtime;

import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import pilots.runtime.*;


public class DataReceiver implements Runnable  {
    private static Logger LOGGER = Logger.getLogger(SimTimeService.class.getName());
    
    private static int DEFAULT_PORT = 8888;
    private static boolean loop = true;
    private static int globalId = 0;
    private Socket sock;
    private int id;
    
    public DataReceiver(Socket sock) {
        this.sock = sock;
        this.id = globalId++;
    }

    private void threadLog(Level level, String msg) {
        LOGGER.log(level, "(Thread " + id + ") " + msg);
    }

    public void run() {
        threadLog(Level.INFO, "Started");

        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String str = null, varNames = null;
            DataStore dataStore = null;

            while ((str = in.readLine()) != null) {
                if (str.length() == 0) {
                    threadLog(Level.INFO, "EOS marker received");
                    break;
                }
                else if (str.charAt(0) == '#') {
                    threadLog(Level.INFO, "First line received: " + str);
                    varNames = str;
                    synchronized (this) {
                        dataStore = DataStore.getInstance(str);
                    }
                }
                else {
                    if (dataStore == null) {
                        threadLog(Level.WARNING, "No data store");
                        break;
                    }

                    threadLog(Level.FINER, "Data received for \"" + varNames + "\": " + str);
                    dataStore.addData(str);
                }

            }

            in.close();
            sock.close();

        } catch (IOException ex) {
            LOGGER.severe(ex.toString());
        }

        threadLog(Level.INFO, "Finished");
    }

    public static void startServer(int port) {
        loop = true;
        final int serverPort = port;

        // daemon thread listening port 8888
        new Thread() {
            public void run() {
                try {
                    ServerSocket serverSock = new ServerSocket(serverPort);
                    LOGGER.info("Started listening to port:" + serverPort);

                    while (loop) {
                        Socket newSock = serverSock.accept();
                        DataReceiver dataReceiver = new DataReceiver(newSock);
                        Thread t = new Thread(dataReceiver);
                        t.start();
                    } 
                } catch (Exception ex) {
                    LOGGER.severe(ex.toString());                    
                }
            }
        }.start();
    }

    public static void stopServer() {
        loop = false;
    }

    public static void main(String[] args) {
        int port;
        if (args.length == 2) {
            port = Integer.parseInt(args[1]);
        }
        else {
            port = DEFAULT_PORT;
        }
        DataReceiver.startServer(port);
    }
}
