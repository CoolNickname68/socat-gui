package com.example.localproxyconnector;
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
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mainLayout;
    private List<FormStopButtonPair> formStopButtonPairs = new ArrayList<>();
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

        Button addButton = new Button(this);
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

        mainLayout.addView(addButton);

        Button startButton = new Button(this);
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

        mainLayout.addView(startButton);
    }

    private static class FormStopButtonPair {
        private LinearLayout formLayout;
        private Button stopButton;

        public FormStopButtonPair(LinearLayout formLayout, Button stopButton) {
            this.formLayout = formLayout;
            this.stopButton = stopButton;
        }

        public LinearLayout getFormLayout() {
            return formLayout;
        }

        public Button getStopButton() {
            return stopButton;
        }
    }

    private void addDynamicInputFields() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        // Создаем новую форму
        LinearLayout dynamicFormLayout = new LinearLayout(this);
        dynamicFormLayout.setLayoutParams(params);
        dynamicFormLayout.setOrientation(LinearLayout.VERTICAL);

        EditText remoteIpEditText = new EditText(this);
        remoteIpEditText.setHint("Enter Remote IP");
        dynamicFormLayout.addView(remoteIpEditText, params);

        EditText remotePortEditText = new EditText(this);
        remotePortEditText.setHint("Enter Remote Port");
        dynamicFormLayout.addView(remotePortEditText, params);

        EditText localPortEditText = new EditText(this);
        localPortEditText.setHint("Enter Local Port");
        dynamicFormLayout.addView(localPortEditText, params);

        Button stopButton = new Button(this);
        stopButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        stopButton.setText("Stop Server");

        mainLayout.addView(dynamicFormLayout);
        mainLayout.addView(stopButton);

        // Сохраняем связь между формой и кнопкой Stop Server
        FormStopButtonPair pair = new FormStopButtonPair(dynamicFormLayout, stopButton);
        formStopButtonPairs.add(pair);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServer(pair);
            }
        });
    }

    private void startServers() {
        for (FormStopButtonPair pair : formStopButtonPairs) {
            String remoteHost = ((EditText) pair.getFormLayout().getChildAt(0)).getText().toString();
            int remotePort = Integer.parseInt(((EditText) pair.getFormLayout().getChildAt(1)).getText().toString());
            int localPort = Integer.parseInt(((EditText) pair.getFormLayout().getChildAt(2)).getText().toString());

            ProxyServerTask proxyServerTask = new ProxyServerTask(localPort, remoteHost, remotePort);
            proxyServerTasks.add(proxyServerTask);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                proxyServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                proxyServerTask.execute();
            }
        }
    }

    private void stopServer(FormStopButtonPair pair) {
        ProxyServerTask proxyServerTask = findProxyServerTask(pair);
        if (proxyServerTask != null) {
            proxyServerTask.cancel(true);

            try {
                proxyServerTask.getServerSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mainLayout.removeView(pair.getFormLayout());
            mainLayout.removeView(pair.getStopButton());

            formStopButtonPairs.remove(pair);
            proxyServerTasks.remove(proxyServerTask);
        }
    }

    private ProxyServerTask findProxyServerTask(FormStopButtonPair pair) {
        int index = formStopButtonPairs.indexOf(pair);
        if (index != -1 && index < proxyServerTasks.size()) {
            return proxyServerTasks.get(index);
        }
        return null;
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

