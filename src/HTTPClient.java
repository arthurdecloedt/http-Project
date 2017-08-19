import java.io.*;
import java.net.Socket;
import java.sql.Time;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HTTPClient {

    private String HTTPCommand;
    private String URI;
    private int Port;

    private String NAME;
    private String SITE;

    String getPATH() {
        return PATH;
    }

    public String PATH;

    // Lists and variables needed for image retrieval
    // and creation of correct paths to these images
    List links = new ArrayList();

    public List getSites() {
        return sites;
    }

    List sites = new ArrayList();
    List endNames = new ArrayList();
    List Paths = new ArrayList();




    // MAIN METHODE
    // executes statements of the form:
    // java HTTPClient HTTPCommand URI Port
    // reads in arguments in the form of:
    // args[] = HTTPCommand URI Port

    public static void main(String args[]) throws Exception {
        HTTPClient client = new HTTPClient();
        client.setHTTPCommand(args[0]);
        client.setURI(args[1]);
        client.setPort(Integer.parseInt(args[2]));
        client.analyseURI();

        Socket clientSocket = new Socket(client.getSite(), client.getPort());

        if (client.getHTTPCommand().equals("GET")) {
            client.GetCommand(clientSocket);
            client.images(clientSocket);

        } else if ((client.getHTTPCommand().equals("PUT")) ||(client.getHTTPCommand().equals("POST")) ) {
            client.putPostCommand(clientSocket);
        }
        clientSocket.close();

        System.out.println("*** Socket closed, program finished \n");
    }

    private void GetCommand(Socket clientSocket) {
        try {
            System.out.println("*** commando uitvoeren: GET");

            File f = new File(this.getName() + ".html");

            HTTPGET get = new HTTPGET(clientSocket, f,this);
            get.GET();

        }
        catch (Exception ex) {
            System.out.println("Error in GETcommand");
            System.out.println(ex.toString());
        }
        System.out.println("*** get commando beëindigd");

    }


    public void analyseURI(){
        this.SITE = getURI();
        this.PATH = "";
        this.NAME = "";
        try {
            String[] parts = getURI().split("/");
            SITE = parts[0];
            PATH = parts[1];

            for (int i=2;i<parts.length; i++) {
                PATH = parts[1]+"/"+parts[i];
            }
        } catch(Exception e){
            System.out.println(e.toString());
            System.out.println("*** een site zonder path.");
        }
        // Retrieve the name of the file requested,
        // if no name is specified this variable is set to SITE.
        int startName = PATH.lastIndexOf("/");
        if ((PATH.contains("/")) || !PATH.isEmpty() ){
            this.NAME =PATH.substring(startName+1);
        } else {
            this.NAME =SITE;
        }
        System.out.println("*** naam van bestand: "+this.NAME);
        System.out.println("*** naam van site:" + this.SITE);
        System.out.println("*** path naam " + this.getPATH());
    }


    public void images(Socket clientSocket){
        int i = getLinks().size()-1; // #images -1
        System.out.println("*** images");

        while(i>=0) try {
            //creating i/o streams
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());

            //composing and sending the request
            String message = "GET /" + getLinks().get(i) + " HTTP/" + 1.1 + "\r\n" + "host: " + SITE + ":" + Port + "\r\n\r\n";
            System.out.println("*** commando: \n" + message);
            outToServer.writeBytes(message);
            outToServer.flush();

            File file;
            File directory;
            //check if file and path already exist, if not create them
            if (!getPaths().get(i - 1).equals("none")) {
                directory = new File(getPaths().get(i).toString());
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                file = new File(directory, getEndNames().get(i - 1).toString());
            } else {
                file = new File(getEndNames().get(i - 1).toString());
            }

            HTTPGetimg getimg = new HTTPGetimg(file,clientSocket,in);
            getimg.read();

        } catch (Exception e) {
            System.out.println("image retrieval fail at nr" + i);
        }


    }

    // Executing the PUT or POST command.
    public void putPostCommand(Socket clientSocket) {
        try {
            System.out.println("*** commando uitvoeren: "+ HTTPCommand);
            BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
            System.out.print("bericht: ");
            String sentence = inFromUser.readLine();

            // Create outputstream (convenient data writer) to this host.
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            // Create an inputstream (convenient data reader) to this host
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // composition of the message that is going to be sent to the server.
            String message = HTTPCommand+ " /" +PATH+ " HTTP/1.1" + "\r\n" + "host: "+ SITE +":" +Port + "\r\n\r\n"+ sentence +"\r\n";
            System.out.println("*** commando: \n"+message);
            //sending the message to the server.
            outToServer.writeBytes(message);
            System.out.println("*** commando verzonden aan server.");
            System.out.println("*** antwoord van de server:");
            outToServer.flush();

            // Read text from the server and write it to the screen.
            String answer;
            answer = null;
            answer = inFromServer.readLine();
            System.out.println(answer);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Setters
    public void setHTTPCommand(String arg) {
        this.HTTPCommand = arg;
    }
    public void setURI(String arg) {
        this.URI = arg;
    }
    public void setPort(int arg) {
        this.Port = arg;
    }

    //Getters
    public String getURI() {
        return this.URI;
    }
    public int getPort() {
        return this.Port;
    }
    public String getHTTPCommand() {
        return this.HTTPCommand;
    }
    public String getSite() {return this.SITE;}
    public String getName() {return this.NAME;}
    public List getLinks(){
        return links;
    }
    public List getEndNames(){
        return endNames;
    }
    public List getPaths(){
        return Paths;
    }
}
