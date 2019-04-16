/*
Copyright (c) 2011, Marcos Diez --  marcos AT unitron.com.br
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Neither the name of  Marcos Diez nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.CodingCult.shareit;

//package webs;

import android.util.Log;

import com.CodingCult.shareit.activities.BaseActivity;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MyHttpServer extends Thread {



    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static int port;
    private static ArrayList<UriInterpretation> fileUris;
    private static ServerSocket serverSocket = null;
    private static BaseActivity launcherActivity = null;
    private boolean webserverLoop = true;



    // default port is 80
    public MyHttpServer(int listen_port){
        port = listen_port;
        if (serverSocket == null) {
            this.start();
        }
    }

    public static void setBaseActivity(BaseActivity baseActivity){
        MyHttpServer.launcherActivity = baseActivity;
    }

    public static void setFiles(ArrayList<UriInterpretation> fileUris) {
        MyHttpServer.fileUris = fileUris;
    }

    public static ArrayList<UriInterpretation> GetFiles() {
        return MyHttpServer.fileUris;
    }

    public static String getLocalIpAddress() {
        try {
            InetAddress localAddress = null;
            InetAddress ipv6 = null;

            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet6Address) {
                        ipv6 = inetAddress;
                        continue;
                    }
                    if (inetAddress.isLoopbackAddress()) {
                        localAddress = inetAddress;
                        continue;
                    }
                    return inetAddress.getHostAddress().toString();
                }
            }
            if (ipv6 != null) {
                return ipv6.getHostAddress().toString();
            }
            if (localAddress != null) {
                return localAddress.getHostAddress().toString();
            }
            return "0.0.0.0";

        } catch (SocketException ex) {
            Log.e("httpServer", ex.toString());
        }
        return "0.0.0.0";
    }

    private String getServerUrl(String ipAddress) {
        if (port == 80) {
            return "http://" + ipAddress + "/";
        }
        if (ipAddress.indexOf(":") >= 0) {
            // IPv6
            int pos = ipAddress.indexOf("%");
            // java insists in adding %wlan and %p2p0 to everything
            if (pos > 0) {
                ipAddress = ipAddress.substring(0, pos);
            }
            return "http://[" + ipAddress + "]:" + port + "/";
        }
        return "http://" + ipAddress + ":" + port + "/";
    }

    public synchronized void stopServer() {
        s("Closing server...\n\n");
        webserverLoop = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
        }
    }

    public CharSequence[] listOfIpAddresses() {
        ArrayList<String> arrayOfIps = new ArrayList<String>();


        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();

                Log.d(Util.myLogName, "Inteface: " + intf.getDisplayName());
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    String theIpTemp = inetAddress.getHostAddress();
                    String theIp = getServerUrl(theIpTemp);

                    if (inetAddress instanceof Inet6Address
                            || inetAddress.isLoopbackAddress()) {

                        arrayOfIps.add(theIp);
                        continue;
                    }

                    arrayOfIps.add(0, theIp); // we prefer non local IPv4
                }
            }

            if (arrayOfIps.size() == 0) {
                String firstIp = getServerUrl("0.0.0.0");
                arrayOfIps.add(firstIp);
            }

        } catch (SocketException ex) {
            Log.e("httpServer", ex.toString());
        }

        CharSequence[] output = arrayOfIps.toArray(new CharSequence[arrayOfIps
                .size()]);
        return output;
    }

    private boolean normalBind(int thePort) {
        s("Attempting to bind on port " + thePort);
        try {
            serverSocket = new ServerSocket(thePort);
        } catch (Exception e) {
            s("Fatal Error:" + e.getMessage() + " " + e.getClass().toString());
            return false;
        }
        port = thePort;
        s("Binding was OK!");
        return true;
    }

    public void run() {
        s("Starting " + Util.myLogName + " server v" + BuildConfig.VERSION_NAME);
        if (!normalBind(port)) {
            return;
        }

        // go in a infinite loop, wait for connections, process request, send
        // response
        while (webserverLoop) {
            s("Ready, Waiting for requests...\n");
            try {
                Socket connectionSocket = serverSocket.accept();
                HttpServerConnection theHttpConnection = new HttpServerConnection(fileUris, connectionSocket, launcherActivity);

                threadPool.submit(theHttpConnection);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    private void s(String s2) { // an alias to avoid typing so much!
        Log.d(Util.myLogName, s2);
    }



}
