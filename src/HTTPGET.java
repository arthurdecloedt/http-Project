import java.io.*;
import java.net.Socket;


public class HTTPGET {

    //necessary files
    private Socket socket;
    private String begin = "<html>";
    private String end = "</html>";
    private File file;
    private HTTPClient client;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;

    //constructor
    public HTTPGET(Socket socket, File file, HTTPClient client) throws Exception {
        this.socket = socket;
        this.file = file;
        this.client = client;
        outToServer = new DataOutputStream(socket.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void GET() throws Exception {
        String mod = checkLastMod();

        Writer writer;
        System.out.println("Get methode working");
        System.out.println(client.getPATH());

        String message = "GET /" +client.getPATH()+ " HTTP/" + 1.1 + "\r\n" + "Accept: */*"+ "\r\n"+"User-Agent:  HTTPie/0.9.8" + "\r\n"+ "Content-Type: text/html "+ "\r\n" + "host: "+ client.getSite() +":" +client.getPort() +"\r\n"+"If-Modified-Since: "+mod+ "\r\n\r\n";
        if ( mod==null) { // when the site is visited the first time
            // the message doesn't contains the If-Modified-Since request.
            message = "GET /" +client.getPATH()+ " HTTP/" + 1.1 + "\r\n" + "User-Agent:  HTTPie/0.9.8"+ "\r\n"+ "Accept: */*" /* + "\r\n" + "Accept-Encoding: gzip, deflate "*/+ "\r\n" + "host: "+ client.getSite() +":" +client.getPort() +"\r\n" +"Connection: keep-alive"+ "\r\n\r\n";
        }

        System.out.println(message);
        outToServer.writeBytes(message);
        String answer;
        answer = inFromServer.readLine();
        String firstAnswer = answer;

        if (firstAnswer.contains("304")){
            return;
        }
        if (file.exists())file.delete();
        file.createNewFile();
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(client.getName()+".html",true), "utf-8"));
        boolean inbody=false;

        System.out.println(answer);
        while (inFromServer.ready()){
            System.out.println(answer);
            answer=inFromServer.readLine();
            if (answer.toLowerCase().contains(begin)){
                inbody=true;
                System.out.println("html start");
            }
            if (!inbody && answer.contains("Last-Modified")) setmodif(answer);
            if(inbody){
                writer.append(answer).append("\n");
                writer.flush();
            }
            if (inbody) if (answer.toLowerCase().contains(end)) break;
            String SITE2=null;

            while (answer.toLowerCase().contains("<img")) {
                System.out.println("*** Bevat embedded object: " + answer);
                // find image link
                int startSrc = answer.toLowerCase().indexOf("src=\"");
                int beginSrc = startSrc +5;
                String link1 = answer.substring(startSrc+5);
                int stopSrc = link1.indexOf("\"");
                String link2 = link1.substring(0,stopSrc);
                System.out.println("*** URL van gevonden afbeelding: " + link2);
                // depending on the form of the link,
                // different methodes are applied to get the proper link.
                if (link2.substring(0,4).toLowerCase().equals("http")) {
                    link2= link2.substring(7);
                    startSrc = link2.toLowerCase().indexOf("/");
                    SITE2 = link2.substring(0,startSrc);
                    System.out.println("SITE: "+SITE2);
                    link2= link2.substring(startSrc+1);
                    System.out.println("*** link begon met HTTP: "+link2);
                }
                if (link2.substring(0,4).toLowerCase().equals("www.")) {
                    startSrc = link2.toLowerCase().indexOf("/");
                    SITE2 = link2.substring(0,startSrc);
                    link2= link2.substring(startSrc);
                    System.out.println("*** link begon met WWW: "+link2);
                }

                // the link is added to the list containing all links
                client.getLinks().add(link2);
                System.out.println("*** link toegevoegd aan lijst LINKS: "+ link2);
                client.getSites().add(SITE2);
                System.out.println("*** site toegevoegd aan lijst SITES: "+ SITE2);

                // the name of the image is stored in a list to later name the file correctly.
                startSrc = link2.lastIndexOf("/");
                client.getEndNames().add(link2.substring(startSrc+1));
                System.out.println("*** naam toevevoegd aan lijst EndNames: "+ link2.substring(startSrc+1));

                if (link2.contains("/")) {
                    client.getPaths().add(link2.substring(0,startSrc));
                    System.out.println("*** Path toegevoegd aan lijst Paths: "+ link2.substring(0,startSrc));
                } else {
                    client.getPaths().add("none");
                    System.out.println("*** Geen path toegevoegd aan lijst Paths: afbeelding bevind zich in currect directory ");
                }

                answer = answer.substring(beginSrc);
            }

        }

    }

    private void setmodif(String answer) {

        try {
            assert answer.contains("Last-Modified");
            BufferedWriter writerHead = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Last-modified.txt", true), "utf-8"));
            String newdate = answer.substring(answer.indexOf("Last-Modified:")+15);
            writerHead.append(client.getSite()).append(": ").append(newdate).append("\r\n");	// add the site with the correct date.
            writerHead.close();
        }
        catch (Exception e){
            System.out.println("Writing last modified failed");
        }
    }

    private String checkLastMod() throws Exception {
        String date;
        BufferedReader in=null;
        try {
            File lastmod = new File("Last-modified.txt");

            if (!lastmod.exists()) {
                lastmod.createNewFile();
                return null;
            }

            in = new BufferedReader(new InputStreamReader(new FileInputStream("Last-modified.txt"), "utf-8"));
            String ln;
            ln = in.readLine();
            int i=1;
            while (in.ready()&&i++<40) {

                if (ln.contains(client.getSite())) {
                    date = ln.substring(ln.indexOf(client.getSite()) + client.getSite().length() + 2);
                    in.close();
                    return date;
                }


            }
            in.close();
            return null;
        }
        catch (Exception e){
            try{in.close();}
            catch(Exception ex){}
            return null;
        }

    }

}
