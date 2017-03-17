package com.example.yuiza.webrtc_controller;

import io.skyway.Peer.*;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private Peer            _peer;
    private String          _id;
    private DataConnection  _data;
    private Boolean         _open;
    private Handler         _handler;
    private String[]        _listPeerIDs;

    private SensorManager sensorManager;
    private static final int MAX = 3;

    private Boolean         _mode;
    private Button          btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _handler = new Handler(Looper.getMainLooper());
        Context context = getApplicationContext();


        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        // gyro sensor
        Sensor Gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, Gyro, SensorManager.SENSOR_DELAY_UI);

        // accele sensor
        Sensor Accele = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, Accele, SensorManager.SENSOR_DELAY_UI);

        PeerOption options = new PeerOption();

        options.key = "SkyWayから取得";
        options.domain = "SkyWayから取得";
        _peer = new Peer(context, options);

        PeerEventCallback(_peer);

        //
        // UI parts
        //

        // connect button
        _open = false;
        final Button btnConnect = (Button)findViewById(R.id.btnConnect);
        if(null != btnConnect){
            btnConnect.setText("接続");
            btnConnect.setEnabled(true);
            btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){

                    if(!_open){
                        getListPeers();
                        btnConnect.setText("切断");
                    }else{
                        close();
                        btnConnect.setText("接続");
                    }

                    v.setEnabled(true);
                }
            });

        }
        // disconnect button
//        Button btnDisconnect = (Button)findViewById(R.id.btnDisconnect);
//        if(null != btnDisconnect){
//            btnDisconnect.setText("切断");
//            btnDisconnect.setEnabled(true);
//            btnDisconnect.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v){
//                    v.setEnabled(false);
//
//                    if(!_open){
//                        close();
//                    }
//
//                    v.setEnabled(false);
//                }
//            });
//        }

        // disconnect button
        _mode = false;
        btnSend = (Button)findViewById(R.id.send);
        btnSend.setText("視点操作モード");
        btnSend.setOnTouchListener(new Button.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int type = event.getAction();
                Log.d("DEBUG", "TOUCH");
                switch (type) {
                    case MotionEvent.ACTION_DOWN:
                        _mode = true;
                        btnSend.setBackgroundColor(Color.GREEN);
                        break;

                    case MotionEvent.ACTION_UP:
                        _mode = false;
                        btnSend.setBackgroundColor(Color.LTGRAY);
                        break;
                }
                return true;
            }
        });


        Button btnH = (Button)findViewById(R.id.btnH);
        if(null != btnConnect){
            btnH.setText("原宿");
            btnH.setEnabled(true);
            btnH.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){

                    if(_open) {
                        send("H");
                    }
                }
            });
        }

        Button btnG = (Button)findViewById(R.id.btnG);
        if(null != btnConnect){
            btnG.setText("ゴールデンゲートブリッジ");
            btnG.setEnabled(true);
            btnG.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){

                    if(_open) {
                        send("G");
                    }
                }
            });
        }

        Button btnM = (Button)findViewById(R.id.btnM);
        if(null != btnConnect){
            btnM.setText("ミラノ");
            btnM.setEnabled(true);
            btnM.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){

                    if(_open) {
                        send("M");
                    }
                }
            });
        }

    }

    void connect(String peerId){
        if(null == _peer)
            return;


        // connecting peer close
        if(null != _data){
            _data.close();
            _data = null;
        }

        //connectig new peer
        ConnectOption option = new ConnectOption();
        option.metadata = "dc";
        option.label = "android webrtc SDK";
        option.serialization = DataConnection.SerializationEnum.BINARY;

        // connect
        _data = _peer.connect(peerId, option);

        if(null != _data){
            DCCallback(_data);
        }
    }

    void close(){
        if(!_open)
            return;

        _open = false;
        if(null != _data)
            _data.close();
    }

    void send(String str){
        String msg = str;
        Log.d("SEND", str);

        boolean result = _data.send(msg);
        if(result){
            Log.d("DEBUG", "sending -> msg");
        }
    }

    private void PeerEventCallback(Peer peer){
        // Peer Open Event
        peer.on(Peer.PeerEventEnum.OPEN, new OnCallback(){
            @Override
            public void onCallback(Object o){
                // get my id
                if(o instanceof String){
                    _id = (String) o;
                    Log.d("DEBUG", _id);
//                    _handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            Log.d("TAG", "My ID is" + _id);
//                        }
//                    });
                }
            }
        });

        // Peer Connection Event
        peer.on(Peer.PeerEventEnum.CONNECTION, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                if(!(o instanceof DataConnection))
                    return;

                _data = (DataConnection) o;
                DCCallback(_data);
                _open = true;

                //update ui
            }
        });

        peer.on(Peer.PeerEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                // close event TODO
            }
        });

        peer.on(Peer.PeerEventEnum.DISCONNECTED, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                // disconnected event TODO
            }
        });

        peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                // error event TODO
            }
        });
    }

    void unsetPeerCallback(Peer peer) {
        peer.on(Peer.PeerEventEnum.OPEN, null);
        peer.on(Peer.PeerEventEnum.CONNECTION, null);
        peer.on(Peer.PeerEventEnum.CALL, null);
        peer.on(Peer.PeerEventEnum.CLOSE, null);
        peer.on(Peer.PeerEventEnum.DISCONNECTED, null);
        peer.on(Peer.PeerEventEnum.ERROR, null);
    }

    // DataConnection callback
    void DCCallback(DataConnection data){
        data.on(DataConnection.DataEventEnum.OPEN, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                _open = true;
            }
        });

        data.on(DataConnection.DataEventEnum.DATA, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Log.d("DEBUG", (String)o);
                if(o instanceof String) {

//                }else if(o instanceof Double){

                }else if(o instanceof Map){

//                }else if(o instanceof byte[]){

                }
            }
        });

        data.on(DataConnection.DataEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                _data = null;
                // view flag
                // disconnected();
            }
        });

        data.on(DataConnection.DataEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                PeerError error = (PeerError) o;
                Log.d("ERROR", "DataError" + error);
            }
        });
    }

    void unsetDataCallback(DataConnection data)
    {
        data.on(DataConnection.DataEventEnum.OPEN, null);
        data.on(DataConnection.DataEventEnum.DATA, null);
        data.on(DataConnection.DataEventEnum.CLOSE, null);
        data.on(DataConnection.DataEventEnum.ERROR, null);
    }

    private void destroyPeer(){
        if(null != _data){
            unsetDataCallback(_data);
            _data = null;
        }

        if (null != _peer)
        {
            unsetPeerCallback(_peer);

            if (false == _peer.isDisconnected)
            {
                _peer.disconnect();
            }

            if (false == _peer.isDestroyed)
            {
                _peer.destroy();
            }

            _peer = null;
        }
    }

    void getListPeers(){
        Log.d("TAG", "calling getListPeers()");

        if((null == _peer) || (null == _id) || (0 == _id.length())) {
            Log.d("DEBUG", "OMG");
            return;
        }


        _peer.listAllPeers(new OnCallback() {
            @Override
            public void onCallback(Object o) {
                if (!(o instanceof JSONArray)) {
                    Log.d("DEBUG", "RETURN");
                    return;
                }

                JSONArray peers = (JSONArray) o;

                StringBuilder list = new StringBuilder();
                for(int i = 0; peers.length() > i; i++){
                    String str = "";
                    try{
                        str = peers.getString(i);
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                    // my id?
                    if(0 ==_id.compareToIgnoreCase(str))
                        continue;

                    if(0 < list.length())
                        list.append(",");

                    list.append(str);
                }

                String strList = list.toString();
                _listPeerIDs = strList.split(",");

                if((null != _listPeerIDs) && (0 < _listPeerIDs.length)) {
                    Log.d("DEBUG", "SELECT");
                    peerSelect();
                }
            };
        });
    }

    void peerSelect(){
        if(null == _handler) {
            Log.d("DEBUG", "_HANDLER");
            return;
        }

        _handler.post(new Runnable() {
            @Override
            public void run() {
                android.app.FragmentManager mgr = getFragmentManager();

                PeerListDialogFragment dialog = new PeerListDialogFragment();

                dialog.setListener(
                        new PeerListDialogFragment.PeerListDialogFragmentListener() {
                            @Override
                            public void onItemClick(final String item) {

                                _handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        connect(item);
                                    }
                                });
                            }
                        });
                dialog.setItems(_listPeerIDs);

                dialog.show(mgr, "peerlist");
            }
        });
    }



    @Override
    protected void onDestroy()
    {
        destroyPeer();

        _listPeerIDs = null;

//        if (null != _handler)
//        {
//            _handler.removeCallbacks(_runAddLog);
//        }

        _handler = null;

        super.onDestroy();
    }

    public void onSensorChanged(SensorEvent event) {
        switch ( event.sensor.getType() ){
            case Sensor.TYPE_ACCELEROMETER :
//                Log.d("ACCEL", "x" + (int)event.values[0]);
//                Log.d("ACCEL", "y" + (int)event.values[1]);
//                Log.d("ACCEL", "z" + (int)event.values[2]);


                int x = (int)event.values[0];
                int y = (int)event.values[1];
                int z = (int)event.values[2];

                //_mode = true => view mode
                if(_mode) {
                    if(_open){
                        ArrayList<Integer> arr = new ArrayList<>();
                        for(int i = 0; i < 3; i++){
                            arr.add((int)event.values[i]);
                        }
                        boolean arr_result = _data.send(arr);

                        if(false == arr_result){
                            Log.e("ERROR", "Array send");
                        }
                    }

                //_mode = false => move mode
                }else {
                    if (x == 0 && y < -1) {
                        Log.d("ROUTE", "北");
                        if (_open)
                            send("N");

                    } else if (x == 0 && y > 1) {
                        Log.d("ROUTE", "南");
                        if (_open)
                            send("S");

                    } else if (x < -2 && y == 0) {
                        Log.d("ROUTE", "東");
                        if (_open)
                            send("E");

                    } else if (x > 2 && y == 0) {
                        Log.d("ROUTE", "西");
                        if (_open)
                            send("W");

                    } else if (x <= -1 && y <= -1) {
                        Log.d("ROUTE", "北東");
                        if (_open)
                            send("NE");

                    } else if (x >= 1 && y <= -1) {
                        Log.d("ROUTE", "北西");
                        if (_open)
                            send("NW");

                    } else if (x <= -1 && y >= 1) {
                        Log.d("ROUTE", "南東");
                        if (_open)
                            send("SE");

                    } else if (x >= 1 && y >= 1) {
                        Log.d("ROUTE", "南西");
                        if (_open)
                            send("SW");
                    }
                }
                break;
            case Sensor.TYPE_GYROSCOPE :
//                if(event.values[0] > MAX) {
//                    Log.d("DEBUG", "FORWARD");
//                    // x
//                    if(_open)
//                        send("FORWARD");
//                }else if(event.values[0] < -1 * MAX){
//                    Log.d("DEBUG", "BACK");
//                    // -x
//                    // SEND [_data.send ]TODO
//                    if(_open)
//                        send("BACK");
//                }else if(event.values[1] > MAX) {
//                    Log.d("DEBUG", "RIGHT");
//                    // y
//                    if(_open)
//                        send("RIGHT");
//                }else if(event.values[1] < -1 * MAX){
//                    Log.d("DEBUG", "LEFT");
//                    // -y
//                    if(_open)
//                        send("LEFT");
                //}else
//                if(event.values[2] > MAX) {
//                    Log.d("DEBUG", "LEFT_ROLL");
//                    // z
//                    if(_open)
//                        send("LEFT_ROLL");
//                }else if(event.values[2] < -1 * MAX){
//                    Log.d("DEBUG", "RIGTH_ROLL");
//                    // -z
//                    if(_open)
//                        send("RIGHT_ROLL");
//                }
                break;
        }
    }

    public void onAccuracyChanged(Sensor sensor,int accuracy) {

    }
}
