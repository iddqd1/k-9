package pl.wp.cloud.service.helper;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Transfer {

    public static Integer sendFileToServer(String urlString, String... file) {
        Integer ret = 0;
        int maxBufferSize = 1 * 256 * 1024;
        int fileSize = 0;
        int headerSize = 86;
        int infoSize = 0;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;

        String exsistingFileName = file[0];

        String lineEnd = "\r\n";

        String twoHyphens = "--";
        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;

        try {

            FileInputStream fileInputStream = new FileInputStream(
                    new File(exsistingFileName));

            // open a URL connection to the Servlet
            URL url = new URL(urlString);

            //Open a HTTP connection to the URL
            conn = (HttpURLConnection) url.openConnection();

            // Allow Inputs
            conn.setDoInput(true);

            // Allow Outputs
            conn.setDoOutput(true);

            // Don't use a cached copy.
            conn.setUseCaches(false);

            // Use a post method.
            fileSize = fileInputStream.available();
            infoSize = fileSize + headerSize + file[0].length();
            conn.setFixedLengthStreamingMode(infoSize);
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data;name='uploadedfile';filename=" + file[0] + "" + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            int bytesSent = 0;
            while (bytesRead > 0) {
                if (bytesSent > 0) {
                    int pg = (bytesSent * 100) / infoSize;
                    //publishProgress(pg);
                }
                bytesSent += bufferSize;
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            //close streams
            fileInputStream.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            ret = 1;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ret = 1;
        }

        try {
            inStream = new DataInputStream(conn.getInputStream());
            String str;

            while ((str = inStream.readLine()) != null) {
                //Log.i("MyAPP","Server Response"+str);
            }
            inStream.close();
        } catch (IOException ioex) {
            ioex.printStackTrace();
            ret = 1;
        }

        return ret;
    }
}
