package com.example.localproxyconnector;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class ProxyServerTask extends AsyncTask<Void, Void, Void> {
    private int localPort;
    private String remoteHost;
    private int remotePort;
    private static final String TAG = "ProxyServerTask";
    private ServerSocket serverSocket;
    private boolean isRunning = true; // Флаг для отслеживания состояния сервера

    public ProxyServerTask(int localPort, String remoteHost, int remotePort) {
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    // Метод для остановки сервера
    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            serverSocket = new ServerSocket(localPort);
            Log.d(TAG, "Proxy server listening on port " + localPort);

            while (isRunning) { // Проверяем флаг состояния сервера
                Socket clientSocket = serverSocket.accept();
                Log.d(TAG, "Accepted connection from " + clientSocket.getInetAddress());

                Socket remoteSocket = new Socket(remoteHost, remotePort);

                new Thread(() -> {
                    try {
                        // Перенаправление от клиента к серверу
                        redirect(clientSocket.getInputStream(), remoteSocket.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                new Thread(() -> {
                    try {
                        // Перенаправление от сервера к клиенту
                        redirect(remoteSocket.getInputStream(), clientSocket.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void redirect(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
}