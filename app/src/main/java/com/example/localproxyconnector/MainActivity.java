package com.example.localproxyconnector;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

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
    private List<Button> dynamicStopButtons = new ArrayList<>();
    private Button addButton;
    private Button startButton;
    private List<FormStopButtonPair> formStopButtonPairs = new ArrayList<>();
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(mainLayout);

        handler = new Handler(Looper.getMainLooper());

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

        mainLayout.addView(addButton);

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

        mainLayout.addView(startButton);
    }

    private static class FormStopButtonPair {
        private LinearLayout formLayout;
        private Button stopButton;
        private ProxyServerTask proxyServerTask;

        public FormStopButtonPair(LinearLayout formLayout, Button stopButton, ProxyServerTask proxyServerTask) {
            this.formLayout = formLayout;
            this.stopButton = stopButton;
            this.proxyServerTask = proxyServerTask;
        }

        public LinearLayout getFormLayout() {
            return formLayout;
        }

        public Button getStopButton() {
            return stopButton;
        }

        public ProxyServerTask getProxyServerTask() {
            return proxyServerTask;
        }
    }

    private void addDynamicInputFields() {
        LinearLayout dynamicFormLayout = new LinearLayout(this);
        dynamicFormLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        dynamicFormLayout.setOrientation(LinearLayout.VERTICAL);

        EditText remoteIpEditText = new EditText(this);
        remoteIpEditText.setHint("Enter Remote IP");
        dynamicFormLayout.addView(remoteIpEditText);

        EditText remotePortEditText = new EditText(this);
        remotePortEditText.setHint("Enter Remote Port");
        dynamicFormLayout.addView(remotePortEditText);

        EditText localPortEditText = new EditText(this);
        localPortEditText.setHint("Enter Local Port");
        dynamicFormLayout.addView(localPortEditText);

        Button stopButton = new Button(this);
        stopButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        stopButton.setText("Stop Server");

        dynamicStopButtons.add(stopButton);

        FormStopButtonPair pair = new FormStopButtonPair(dynamicFormLayout, stopButton, null);
        formStopButtonPairs.add(pair);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServer(pair);
            }
        });

        mainLayout.addView(dynamicFormLayout);
        mainLayout.addView(stopButton);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            dynamicFormLayout.post(new Runnable() {
                @Override
                public void run() {
                    startDynamicServer(pair, localPortEditText.getText().toString(), remoteIpEditText.getText().toString(), remotePortEditText.getText().toString());
                }
            });
        } else {
            startDynamicServer(pair, localPortEditText.getText().toString(), remoteIpEditText.getText().toString(), remotePortEditText.getText().toString());
        }
    }

    private void startDynamicServer(FormStopButtonPair pair, String localPort, String remoteIp, String remotePort) {
        try {
            int port = Integer.parseInt(localPort);
            int remotePortInt = Integer.parseInt(remotePort);

            if (isPortAvailable(port)) {
                ProxyServerTask proxyServerTask = new ProxyServerTask(port, remoteIp, remotePortInt);
                pair.proxyServerTask = proxyServerTask;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    proxyServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    proxyServerTask.execute();
                }
            } else {
                showToast("Port is not available");
            }
        } catch (NumberFormatException e) {
            showToast("Invalid port number");
        }
    }

    private void startServers() {
        for (FormStopButtonPair pair : formStopButtonPairs) {
            startDynamicServer(pair, ((EditText) pair.getFormLayout().getChildAt(2)).getText().toString(),
                    ((EditText) pair.getFormLayout().getChildAt(0)).getText().toString(),
                    ((EditText) pair.getFormLayout().getChildAt(1)).getText().toString());
        }
    }

    private void stopServer(FormStopButtonPair pair) {
        ProxyServerTask proxyServerTask = pair.getProxyServerTask();
        if (proxyServerTask != null) {
            proxyServerTask.cancel(true);

            try {
                ServerSocket serverSocket = proxyServerTask.getServerSocket();
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            mainLayout.removeView(pair.getFormLayout());
            mainLayout.removeView(pair.getStopButton());

            formStopButtonPairs.remove(pair);
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

    private boolean isPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void showToast(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
