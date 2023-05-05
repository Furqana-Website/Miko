package com.gtappdevelopers.miko;

import static com.miko3.aidl_lib.interfaces.GlobalAIDL.DELIMETER;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ActionMenuView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.miko3.aidl_lib.AIDLProcess;
import com.miko3.aidl_lib.interfaces.AIDLCallbackListner;
import com.miko3.aidl_lib.models.ClientData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class ClientServer extends AppCompatActivity implements View.OnClickListener, AIDLCallbackListner {

    public static final int SERVER_PORT = 8080;
    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    private Thread serverThread;
    private TextView ip;
    public static String SERVER_IP = "";
    private LinearLayout msgList;
    private Handler handler;
    private EditText edMessage;
    QRGEncoder qrgEncoder;
    VideoView videoView;
    ImageView qrCodeIV;
    boolean connected;
    RelativeLayout serverrelativeLayout;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        setTitle("Server");
        qrCodeIV=findViewById(R.id.idIVQrcode);
        videoView=findViewById(R.id.videoView);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        serverrelativeLayout=findViewById(R.id.serverrelative);
        edMessage = findViewById(R.id.edMessage);
        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
//        ip.setText("IP: " + SERVER_IP+"\tPort Number:"+SERVER_PORT);

        ActionBar actionBar = getSupportActionBar();

        // Customize the back button
        actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);

        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);
        AIDLProcess.INSTANCE.initAIDL(getApplication(),this,500);
        MediaController mediaController= new MediaController(this);
        mediaController.setAnchorView(videoView);

        //specify the location of media file
        Uri uri=Uri.parse(Environment.getExternalStorageDirectory().getPath()+"/klug/APPS/images/love_eyes.mp4");

        //Setting MediaController and URI, then starting the videoView
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(uri);
        videoView.requestFocus();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });
        videoView.start();
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


        private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }
    public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }


        String m = message + " [" + getTime() +"]";
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(m);
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }

    public void showMessage(final String message, final int color) {
        handler.post(() ->
                msgList.addView(textView(message, color)));
                if(message.contains("Server Started"))
                {
                    AIDLProcess.INSTANCE.Speak("Server Started");
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(message.contains("Connected"))
                        {
                            connected=true;
                            serverrelativeLayout.setBackground(getApplicationContext().getDrawable(R.drawable.weare));
                            AIDLProcess.INSTANCE.Speak("We are connected");
                        }
                        else if(message.contains("Disconnected"))
                        {
                            serverrelativeLayout.setBackground(getApplicationContext().getDrawable(R.drawable.diconnect));
                            findViewById(R.id.relative).setVisibility(View.GONE);
                            findViewById(R.id.vibratephone).setVisibility(View.GONE);
                            findViewById(R.id.message).setVisibility(View.GONE);
                            findViewById(R.id.heatrate).setVisibility(View.GONE);
                            findViewById(R.id.serverrelative).setVisibility(View.VISIBLE);
                        }


                    }
                });

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.start_server) {
            findViewById(R.id.relative).setVisibility(View.VISIBLE);
            findViewById(R.id.vibratephone).setVisibility(View.GONE);
            findViewById(R.id.message).setVisibility(View.GONE);
            findViewById(R.id.heatrate).setVisibility(View.GONE);
            findViewById(R.id.serverrelative).setVisibility(View.VISIBLE);
            removeAllViews();
            showMessage("Server Started.", Color.GREEN);
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            //initializing a variable for default display.
            Display display = manager.getDefaultDisplay();
            //creating a variable for point which is to be displayed in QR Code.
            Point point = new Point();
            display.getSize(point);
            //getting width and height of a point
            int width = point.x;
            int height = point.y;
            //generating dimension from width and height.
            int dimen = width < height ? width : height;
            dimen = dimen * 3 / 4;
            //setting this dimensions inside our qr code encoder to generate our qr code.
            qrgEncoder = new QRGEncoder(SERVER_IP, null, QRGContents.Type.TEXT, dimen);
            try {
                //getting our qrcode in the form of bitmap.
                Bitmap bitmap = qrgEncoder.encodeAsBitmap();
                // the bitmap is set inside our image view using .setimagebitmap method.
                qrCodeIV.setImageBitmap(bitmap);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.relative).setVisibility(View.GONE);
                    }
                }, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.serverThread = new Thread(new ServerThread());
            this.serverThread.start();
            return;
        }
        if (view.getId() == R.id.send_data) {
            String msg = edMessage.getText().toString().trim();
            showMessage("Server : " + msg, Color.BLUE);
            sendMessage(msg);
        }

        if(view.getId()==R.id.vibrate)
        {
            if(connected) {
                sendMessage("vibrate");
                AIDLProcess.INSTANCE.Speak("Lets make your phone vibrate");
                findViewById(R.id.vibratephone).setVisibility(View.VISIBLE);
                findViewById(R.id.message).setVisibility(View.GONE);
                findViewById(R.id.heatrate).setVisibility(View.GONE);
                findViewById(R.id.serverrelative).setVisibility(View.GONE);
            }
            else{
                AIDLProcess.INSTANCE.Speak("Please Connect to the server");
            }
        }
        if(view.getId()==R.id.heartbeat)
        {
            if(connected) {
                sendMessage("heartbeat");
                AIDLProcess.INSTANCE.Speak("Check my heartbeat");
                findViewById(R.id.vibratephone).setVisibility(View.GONE);
                findViewById(R.id.message).setVisibility(View.GONE);
                findViewById(R.id.heatrate).setVisibility(View.VISIBLE);
                findViewById(R.id.serverrelative).setVisibility(View.GONE);
            }else{
                    AIDLProcess.INSTANCE.Speak("Please Connect to the server");
                }
        }
        if(view.getId()==R.id.sendmessage)
        {
            if(connected) {
                sendMessage("send/receive");
                AIDLProcess.INSTANCE.Speak("Lets chat");
                msgList.removeAllViews();
                findViewById(R.id.vibratephone).setVisibility(View.GONE);
                findViewById(R.id.message).setVisibility(View.VISIBLE);
                findViewById(R.id.heatrate).setVisibility(View.GONE);
                findViewById(R.id.serverrelative).setVisibility(View.GONE);
            }
            else{
                AIDLProcess.INSTANCE.Speak("Please Connect to the server");
            }
        }
        if(view.getId()==R.id.vibrate1)
        {
            sendMessage("vibrate1");
        }
        if(view.getId()==R.id.vibrate2)
        {
            sendMessage("vibrate2");
        }
        if(view.getId()==R.id.vibrate3)
        {
            sendMessage("vibrate3");
        }
        if(view.getId()==R.id.stop)
        {
            sendMessage("stop");
        }
    }

    private void removeAllViews(){
        handler.post(() -> msgList.removeAllViews());
    }

    private void hideStartServerBtn(){
        handler.post(() -> findViewById(R.id.start_server).setVisibility(View.GONE));
    }

    private void sendMessage(final String message) {
        try {
            if (null != tempClientSocket) {
                new Thread(() -> {
                    PrintWriter out = null;
                    try {
                        out = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(tempClientSocket.getOutputStream())),
                                true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out.println(message);
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAIDLError(Exception e) {
        Log.d("TAG", "onAIDLError: Exception: " + e.getMessage());
    }

    @Override
    public String getFirmwareVersion() {
        return null;
    }

    @Override
    public void onDataReceived(String s, ClientData clientData) {
        Log.d("TAG", "onDataReceived: called" + clientData.getType());
        if (clientData.getType().equals("STATE_MACH_INIT_COMPLETE")) {
            try {
                AIDLProcess.INSTANCE.GameEvent(AIDLProcess.INSTANCE.convertToAIDLData(AIDLProcess.STATE_MACHINE, AIDLProcess.ACTIVE_APP + DELIMETER + getApplicationContext().getPackageName()));
            } catch (Exception e) {
                Log.d("TAG", "onDataReceived: " + e.getMessage());
                Log.d("TAG", "onDataReceived: " + e.toString());
            }
        }

    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
//                hideStartServerBtn();
                serverSocket = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Starting Server : " + e.getMessage(), Color.RED);
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage("Error Communicating to Client :" + e.getMessage(), Color.RED);
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private final Socket clientSocket;

        private BufferedReader input;

        @SuppressLint("UseCompatLoadingForDrawables")
        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Connecting to Client!!", Color.RED);
                return;
            }
            showMessage("Connected to Client!!", Color.WHITE);

        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) try {
                String read = input.readLine();
                if (null == read || "Disconnect".contentEquals(read)) {
                    boolean interrupted = Thread.interrupted();
                    read = "Client Disconnected: " + interrupted;
                    connected=false;
                    showMessage("Client : " + read, Color.RED);
                    break;
                }
                connected=true;
                showMessage("Client : " + read, Color.BLUE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    String getTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
        }
    }


}