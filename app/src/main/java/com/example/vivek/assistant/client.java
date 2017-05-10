package com.example.vivek.assistant;

/**
 * Created by vivek on 4/25/2017.
 */
import java.io.*;
import java.net.*;

public class client {
    String serverAdd = "35.154.241.117";
    int port = 1234;
    Socket s;
    void connect() {
        new Thread(new clientThread()).start();
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(s.getOutputStream())),

                        true);

                out.println("Hello");
            }
            catch (UnknownHostException e){
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }




    }
    class clientThread implements Runnable{

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(serverAdd);
                 s = new Socket(serverAddr, port);



                // s.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}


