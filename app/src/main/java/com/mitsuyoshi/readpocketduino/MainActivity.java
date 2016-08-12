package com.mitsuyoshi.readpocketduino;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    TextView mTvDeviceOpen;
    Physicaloid mPhysicaloid;
    Button mBtStart;
    TextView mTvRead;

    String mReadStr;
    Handler mHandler;
    // 効果音
    SoundPool mSoundPool;
    private int soundId_1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mTvDeviceOpen = (TextView) findViewById(R.id.tvDeviceOpen);
        mPhysicaloid = new Physicaloid(getApplicationContext());
        mBtStart = (Button) findViewById(R.id.btStart);
        mTvRead = (TextView) findViewById(R.id.tvRead);

        mHandler = new Handler();
        //効果音
        mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        soundId_1 = mSoundPool.load(this, R.raw.se_maoudamashii_instruments_drum1_cymbal, 1);

    }

    public void onClickStart(View v){

        if(!mPhysicaloid.isOpened()){
            if(mPhysicaloid.open()){
                mTvDeviceOpen.setText("Device opened!");
                mBtStart.setText("Stop");

                mPhysicaloid.addReadListener(new ReadLisener() {
                    boolean isOverThreshold = false;

                    @Override
                    public void onRead(int size) {
                        byte[] buf = new byte[size];

                        mPhysicaloid.read(buf, size);
                        try {
                            mReadStr = new String(buf, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            return;
                        }

                        String [] data = mReadStr.split(",");
                        int[] num = decodePacket(buf, 3);
                        //mReadStr = String.valueOf(num[0])+", "+String.valueOf(num[1])+", "+String.valueOf(num[2]);
                        mHandler.post(new Runnable() {
                                          @Override
                                          public void run() {
                                              mTvRead.setText(mReadStr);
                                          }
                                      });
                        //効果音再生
                        if (num[0] < 200) {
                            isOverThreshold = false;
                        } else if(!isOverThreshold && num[0] >= 200){
                            mSoundPool.play(soundId_1, 1.0F, 1.0F, 0, 0, 1.0F);
                            isOverThreshold = true;
                        }

                    }
                });
            } else {
                mTvDeviceOpen.setText("Cannot open.");
            }
        } else {
            mPhysicaloid.close();
            mTvDeviceOpen.setText("Closed.");
            mBtStart.setText("Start");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mPhysicaloid.isOpened()){
            mPhysicaloid.close();
        }
        mSoundPool.release();
    }

    @Override
    public void onStop(){
        super.onStop();
    }

//    private int decodePacket(byte[] buf){
//        boolean existStx = false;
//        int result =0;
//
//        for(int i=0; i<buf.length; i++){
//            if(!existStx) {
//                if(buf[i]=='s'){
//                    existStx = true;
//                }
//            } else {
//                if(buf[i]=='\r'){
//                    return result;
//                } else {
//                    if('0' <= buf[i] && buf[i] <= '9'){
//                        result = result*10 + (buf[i]-'0');
//                    } else {
//                        return -1;
//                    }
//                }
//            }
//        }
//        return -1;
//    }

    private int[] decodePacket(byte[] buf, int size){
        boolean existStx = false;
        int[] result = new int[size];
        for (int j=0; j<size; j++)
            result[j] = 0;

        int dataNum = 0;

        for(int i=0; i<buf.length; i++){
            if(!existStx) {
                if(buf[i]=='s'){
                    existStx = true;
                }
            } else {
                if(buf[i]=='\n'){
                    break;
                } else if (buf[i]==','){
                    dataNum++;
                    if (dataNum >= 3) break;
                } else {
                    if('0' <= buf[i] && buf[i] <= '9'){
                        result[dataNum] = result[dataNum]*10 + (buf[i]-'0');
                    } else {
                        for (int j=0; j<size; j++)
                            result[j] = -1;
                        break;
                    }
                }
            }
        }
        return result;
    }
}
