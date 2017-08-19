import java.io.*;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by arthurdecloedt on 18/03/17.
 */
public class HTTPGetimg {

    File file;
    Socket socket;
    DataInputStream in;
    FileOutputStream outf;


    public HTTPGetimg(File file, Socket socket, DataInputStream in)throws Exception {
        this.file = file;
        this.socket = socket;
        this.in= in;

        this.outf = new FileOutputStream(file);

    }

    public void read() throws Exception{
        Boolean start=true;

        byte[] buffer = new byte[512];
        String bufferS;
        String out;
        int i;
        String[] values;
        byte[] bytes;



        in.read(buffer);
        bufferS=Arrays.toString(buffer);
        values = bufferS.substring(1, bufferS.length() - 1).split(",");

        bytes = new byte[values.length];

        for (int h=0, len=bytes.length; h<len; h++) {
            bytes[h] = Byte.parseByte(values[h].trim());
        }

        out = new String(bytes);

        while (out.contains("Content-Type")) {
            out = out.substring(out.indexOf("\n")+1);
        }
        out = out.substring(out.indexOf("\n")+1);
        buffer  = out.getBytes();
        outf.write(buffer,0,buffer.length);
        outf.flush();


        while ( (i = in.read(buffer)) != -1 ) {
            outf.write(buffer, 0, i);
            outf.flush();
            buffer = new byte[512];
        }
        outf.close();
    }
}
