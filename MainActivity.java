package com.example.localproxyconnector;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;



import java.io.IOException;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mainLayout;
    private LinearLayout dynamicInputLayout;
    private LinearLayout dynamicStopButtonLayout;
    private List<Button> dynamicStopButtons = new ArrayList<>();
    private Button addButton;
    private Button startButton;

    private List<View> dynamicInputViews = new ArrayList<>();
    private List<ProxyServerTask> proxyServerTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(mainLayout);

        dynamicInputLayout = new LinearLayout(this);
        dynamicInputLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        dynamicInputLayout.setOrientation(LinearLayout.VERTICAL);

        mainLayout.addView(dynamicInputLayout);

        dynamicStopButtonLayout = new LinearLayout(this);
        dynamicStopButtonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        dynamicStopButtonLayout.setOrientation(LinearLayout.VERTICAL);

        mainLayout.addView(dynamicStopButtonLayout);

        addButton = new Button(this);
        addButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        addButton.setText("+");

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDynamicInputFields();
            }
        });

        dynamicInputLayout.addView(addButton);

        startButton = new Button(this);
        startButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        startButton.setText("Start Servers");

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServers();
            }
        });

        dynamicStopButtonLayout.addView(startButton);
    }

    private void addDynamicInputFields() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        EditText remoteIpEditText = new EditText(this);
        remoteIpEditText.setHint("Enter Remote IP");
        dynamicInputLayout.addView(remoteIpEditText, params);

        EditText remotePortEditText = new EditText(this);
        remotePortEditText.setHint("Enter Remote Port");
        dynamicInputLayout.addView(remotePortEditText, params);

        EditText localPortEditText = new EditText(this);
        localPortEditText.setHint("Enter Local Port");
        dynamicInputLayout.addView(localPortEditText, params);

        dynamicInputViews.add(remoteIpEditText);
        dynamicInputViews.add(remotePortEditText);
        dynamicInputViews.add(localPortEditText);

        Button stopButton = new Button(this);
        stopButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        stopButton.setText("Stop Server");
        dynamicStopButtonLayout.addView(stopButton);
        dynamicStopButtons.add(stopButton);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServer(dynamicStopButtons.indexOf(stopButton));
            }
        });
    }

    private void startServers() {
        for (int i = 0; i < dynamicInputViews.size(); i += 3) {
            String remoteHost = ((EditText) dynamicInputViews.get(i)).getText().toString();
            int remotePort = Integer.parseInt(((EditText) dynamicInputViews.get(i + 1)).getText().toString());
            int localPort = Integer.parseInt(((EditText) dynamicInputViews.get(i + 2)).getText().toString());

            ProxyServerTask proxyServerTask = new ProxyServerTask(localPort, remoteHost, remotePort);
            proxyServerTasks.add(proxyServerTask);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                proxyServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                proxyServerTask.execute();
            }
        }
    }



    private void stopServer(int index) {
        if (index < proxyServerTasks.size()) {
            ProxyServerTask proxyServerTask = proxyServerTasks.get(index);
            proxyServerTask.cancel(true);

            // Остановить сервер, добавьте здесь код остановки сервера по индексу
            try {
                proxyServerTask.getServerSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Убрать форму для ввода данных о сервере после выключения
            for (int i = index * 3; i < index * 3 + 3; i++) {
                View viewToRemove = dynamicInputViews.get(index);
                dynamicInputLayout.removeView(viewToRemove);
                dynamicInputViews.remove(index);
            }

            // Убрать кнопку "Stop Server" после выключения
            Button stopButtonToRemove = dynamicStopButtons.get(index);
            dynamicStopButtonLayout.removeView(stopButtonToRemove);
            dynamicStopButtons.remove(index);

            // Убрать задачу сервера из списка
            proxyServerTasks.remove(index);
        }
    }

    private static class ProxyServerTask extends AsyncTask<Void, Void, Void> {

        private int localPort;
        private String remoteHost;
        private int remotePort;
        private ServerSocket serverSocket;

        public ProxyServerTask(int localPort, String remoteHost, int remotePort) {
            this.localPort = localPort;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                serverSocket = new ServerSocket(localPort);

                while (!isCancelled()) {
                    Socket clientSocket = serverSocket.accept();

                    Socket remoteSocket = new Socket(remoteHost, remotePort);

                    new Thread(() -> {
                        try {
                            redirect(clientSocket.getInputStream(), remoteSocket.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();

                    new Thread(() -> {
                        try {
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

        public ServerSocket getServerSocket() {
            return serverSocket;
        }
    }
}
