
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


class HTTPServer {

    public static void main(String[] args) {

        // we want to be able to close this socket in case of errors
        ServerSocket serverSocket;
        String localHostAdress;
        int port=5000;
        List<Socket> clientSocketlist = new ArrayList<Socket>();
        try {
            serverSocket = new ServerSocket(port);
            // we need our address for comparing it to the host header
            localHostAdress = InetAddress.getLocalHost().getHostAddress()+":"+port;
            System.out.println(localHostAdress);

        } catch (Exception e) {
            System.err.println("***Server Socket could not be Created");
            System.out.println("Error message:");
            e.printStackTrace();
            return;
        }
        int nbClients = 0;

        try {
            while (true) {
                //will wait busy when too much threads are active
                while (Thread.activeCount() >= 50) ;


                Socket cSocket = serverSocket.accept();
                // when a client is found it gets it's own handler that gets wrapped in a thread and started
                System.out.println("***Connected to client: " +  cSocket.getRemoteSocketAddress());
                requestHandler handler=new requestHandler(cSocket,nbClients,localHostAdress);
                new Thread(handler).start();
                nbClients++;
                clientSocketlist.add(cSocket);
            }


        }
        catch (Exception e) {
            System.err.println("***Server Socket encountered an error while trying to connect to a client");
            System.out.println(e.toString());
            e.printStackTrace();


            try {
                serverSocket.close();
            } catch (IOException e1) {
                for (Socket client :
                        clientSocketlist) {
                    try {
                        client.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }

                }
                System.err.println("***Server Socket encountered an error while trying close all sockets");
                e1.printStackTrace();
                System.out.println(e1.toString());
            }
        }

    }
}

    class requestHandler implements Runnable {

        // necessary fields
        private final Socket cSocket;
        private final int nbClients;
        private final String hostname;
        private final String clientRemoteHostname;
        private boolean oldhttp;

        //constructor
        requestHandler(Socket cSocket, int nbClients, String hostname) {
            this.cSocket = cSocket;
            this.nbClients = nbClients;
            this.hostname = hostname;
            clientRemoteHostname = (cSocket.getInetAddress()).toString();
            oldhttp = false;
        }

        @Override
        public void run() {
            //thread execution will start here

            BufferedReader inFromClient = null;
            DataOutputStream outToClient = null;

            try {
                System.out.println("Thread nr " + nbClients + " has started");
                System.out.println("Client ip: " + clientRemoteHostname);
                // Create an inputstream (convenient data reader) to this host
                inFromClient = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
                // Create outputstream (convenient data writer) to this host.
                outToClient = new DataOutputStream(cSocket.getOutputStream());

                // first line is the request
                String requestS = inFromClient.readLine();

                System.out.println("*** client request:" + requestS);

                String[] request = requestS.split(" ");
                if (request[2].contains("1.0")) oldhttp = true;
                String HTTPRequest = request[0];
                //every request has it's own appropriate method
                switch (HTTPRequest) {
                    case "GET":
                        get(request, inFromClient, outToClient, false);
                        break;

                    case "PUT":
                    case "POST":
                        put(request, inFromClient, outToClient);
                        break;

                    case "HEAD":
                        get(request, inFromClient, outToClient, true);
                        break;

                    case "DELETE":
                    case "PATCH":
                    case "TRACE":
                    case "OPTONS":

                        forbidden(outToClient);
                        break;

                    default:
                        badRequest(outToClient);


                }


            } catch (Exception e) {

                e.printStackTrace();
                System.out.println("requestHandler nr" + nbClients + "experienced a" + e.getClass().getSimpleName());
                System.out.println("during handling a request");
                System.out.println(e.toString());
                e.printStackTrace();

                try {
                    //this message signals the remote client something has gone wrong
                    outToClient.writeBytes("500 Server Error");
                    System.out.println(" sent \"500 Server Error\" to client ");
                } catch (Exception e1) {
                    System.err.println("could not send an error message to the client");
                    e1.printStackTrace();
                }
            } finally {
                // we have to try to close these even in the case of an error
                try {
                    inFromClient.close();
                    outToClient.close();
                    cSocket.close();
                } catch (Exception e) {
                    System.err.println("the sockets and or in and out streams could not be gracefully closed");
                    e.printStackTrace();
                }
            }

        }

        //this method will respond to illformed requests (ex no or incorrect host, incorrect request line)
        private void badRequest(DataOutputStream outToClient) throws Exception {
            SimpleDateFormat sdf1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            String date = sdf1.format(new Date());
            outToClient.writeBytes("HTTP/1.1 400 Bad Request" + "\n");
            outToClient.writeBytes("Date: " + date + "\n");
            outToClient.flush();

        }

        // this method will respond to well formed but illegal requests (illegal request methods, replacing vital parts...)
        private void forbidden(DataOutputStream outToClient) throws Exception {
            SimpleDateFormat sdf1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            String date = sdf1.format(new Date());

            outToClient.writeBytes("HTTP/1.1 403 Forbidden" + "\n");
            outToClient.writeBytes("Date: " + date + "\n");
            outToClient.flush();

        }


        // this method will put text submitted by remote clients in a text file;
        private void put(String[] request, BufferedReader inFromClient, DataOutputStream outToClient) throws Exception {
            System.out.println("***put/post started");

            if (!checkHost(inFromClient)) {
                badRequest(outToClient);
                return;
            }
            System.out.println("***Hostname confirmed");

            String path = request[1];
            //even though clients can't choose the resulting names these requests are illegal
            if (path.equals("/") || path.equals("/site.html")) {
                forbidden(outToClient);
                System.err.println("*** client nr: " + nbClients + "tried to make an illegal change");
                return;
            }
            // for every "run" of this server the uniqueness can be guarantied
            String name = "File" + request[0] + "nr" + nbClients + "by" + clientRemoteHostname.substring(1) + ".txt";
            System.out.println("filename will be: " + name);
            File file = new File(name);

            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }
            System.out.println("*** File created");
            BufferedWriter outToFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "utf-8"));
            System.out.println("*** File writer created");

            System.out.println("*** in from client reader.ready(): " + inFromClient.ready());

            //if the client has not yet written a request body he now has the time to
            while (!inFromClient.ready()) ;
            String line;

            // TODO: 22/03/17 integrate with content length
            while (inFromClient.ready()) {
                line = inFromClient.readLine();
                System.out.println(line);
                outToFile.write(line);
                outToFile.flush();
            }
            System.out.println(Time.from(Instant.now()));

            outToFile.close();
            System.out.println("*** File contents written to file");

            SimpleDateFormat sdf1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            String date = sdf1.format(new Date());
            // the remote client get's a confirmation of his request
            outToClient.writeBytes("HTTP/1.1 200 OK" + "\n");
            System.out.println("msg1");
            outToClient.writeBytes("Date: " + date + "\n");
            System.out.println("msg2");
            outToClient.writeBytes("Content-Length: " + 0 + "\r\n");
            System.out.println("msg3");
            outToClient.flush();

            System.out.println("*** Response Sent");

        }

        private void get(String[] request, BufferedReader inFromClient, DataOutputStream outToClient, boolean head) throws Exception {
            System.out.println("***GET/HEAD started");

            int result = checkHostModified(inFromClient);
            if (result==0) {
                badRequest(outToClient);
                return;
            }
            if (result==1){
                notmodified(outToClient);
                return;
            }

            System.out.println("***Hostname confirmed");

            String path = request[1];

            //This server serves only one file
            if (!(path.toLowerCase().equals("/") || path.toLowerCase().equals("/index.html") || path.toLowerCase().equals("/index"))) {
                answernotfound(request, inFromClient, outToClient);
                return;
            }

            File site = new File("site.html");

            if (!site.exists()) {
                answernotfound(request, inFromClient, outToClient);
                System.err.print("*** site.html was not found");
                return;
            }
            System.out.println("***Site.html found");

            BufferedReader inFromFile = new BufferedReader(new InputStreamReader(new FileInputStream("site.html"), "utf-8"));

            SimpleDateFormat sdf1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            String date = sdf1.format(new Date());

            SimpleDateFormat sdf2 = new SimpleDateFormat("dd MMM yyyy");
            Date origDate=new Date(2017,03,21);
            String origDateS = sdf2.format(origDate);

            // the response and headers are sent to the remote client
            outToClient.writeBytes("HTTP/1.1 200 OK" + "\r\n");

            outToClient.writeBytes("Last-Modified: " + origDateS + "\r\n");
            outToClient.writeBytes("Content-Type: text/html" + "\r\n");
            outToClient.writeBytes("Date: " + date + "\r\n");
            outToClient.writeBytes("Content-Length: " + site.length() + "\r\n");
            outToClient.writeBytes("\r\n\r\n");

            // a head request does not need a request body
            if (head) {
                outToClient.flush();
                return;
            }
            // all lines from the file are sent to the
            while (inFromFile.ready()) {
                outToClient.writeBytes(inFromFile.readLine() + "\r\n");
            }
            outToClient.flush();


        }

        private void notmodified(DataOutputStream outToClient) throws Exception {
            SimpleDateFormat sdf1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            String date = sdf1.format(new Date());
            System.out.println("*** sending not modified message");
            outToClient.writeBytes("HTTP/1.1 304 Not Modified" + "\n");
            outToClient.writeBytes("Date: " + date + "\n");
            outToClient.flush();
        }
        // this method responds to requests for resources that could not be located

        private void answernotfound(String[] request, BufferedReader inFromClient, DataOutputStream outToClient) throws Exception {
            SimpleDateFormat sdf1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            String date = sdf1.format(new Date());

            outToClient.writeBytes("HTTP/1.1 404 Not Found" + "\n");
            outToClient.writeBytes("Date: " + date + "\n");
            outToClient.flush();

        }


        // this method responds to request for non-existing resources
        private boolean checkHost(BufferedReader inFromClient) throws Exception {
            boolean result = false;
            boolean encounter = false;
            String line = inFromClient.readLine();
            System.out.println(line);
            while (line.contains(":")) {
                if (line.toLowerCase().contains("host: ")) {
                    if (line.toLowerCase().equals("host: " + hostname) && !encounter) {
                        result = true;
                    }
                    encounter = true;
                }
                line = inFromClient.readLine();
                System.out.println(line);

            }
            return result;

        }

        private int checkHostModified(BufferedReader inFromClient) throws Exception {
            // http 1.0 does not require a host header
            boolean result = oldhttp;
            boolean encounter = false;
            boolean nmod=false;

            SimpleDateFormat sdf1 = new SimpleDateFormat("dd MMM yyyy");
            Date origDate=new Date(2017,03,21);

            String line = inFromClient.readLine();
            System.out.println(line);
            while (line.contains(":")) {
                if (line.contains("Last-Modified:")) {
                    String date2 = line.substring(line.indexOf(":"));
                    Date date1 = sdf1.parse(date2);
                    if (date1.equals(origDate)) nmod = true;
                }
                if (line.toLowerCase().contains("host: ")) {
                    if (line.toLowerCase().equals("host: " + hostname) && !encounter) {
                        result = true;
                    }
                    encounter = true;
                }
                line = inFromClient.readLine();
                System.out.println(line);
            }
            if (!result) return 0;
            if (nmod) return 1;
            return 2;
            }





    }

