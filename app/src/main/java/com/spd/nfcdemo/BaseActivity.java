package com.spd.nfcdemo;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 子类在onNewIntent方法中进行NFC标签相关操作。
 * launchMode设置为singleTop或singelTask，保证Activity的重用唯一
 * 在onNewIntent方法中执行intent传递过来的Tag数据
 *
 * @author zzc
 * @date 2019/12/9
 */
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    protected NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //此处adapter需要重新获取，否则无法获取message
        if (mNfcAdapter == null) {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }
        //一旦截获NFC消息，就会通过PendingIntent调用窗口
        if (mPendingIntent == null) {
            mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //设置处理优于所有其他NFC的处理
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
            Log.d("zm", "zzc onResume()");
        }
    }

    @Override
    protected void onPause() {
        //恢复默认状态
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }
}
