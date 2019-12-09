package com.speedata.nfcdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.nfcdemo.utils.StringUtils;

import java.util.Arrays;

/**
 * @author zzc
 * @date 2019/12/9
 */
public class MainActivity extends BaseActivity {

    private TextView tvTech;
    private TextView tvId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvTech = findViewById(R.id.tv_nfc_tech);
        tvId = findViewById(R.id.tv_nfc_id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // NFC适配器，所有的关于NFC的操作从该适配器进行
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!ifNFCUse()) {
            Log.d("zzc", "mNfcAdapter: false");
            return;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }
        //获取标签类型
        String[] techList = tag.getTechList();
        tvTech.setText("");
        for (String tech : techList) {
            tvTech.append(tech + "\n");
        }
        //获取标签ID
        byte[] byteId = tag.getId();
        String strId = StringUtils.byteArrayToString(byteId);
        tvId.setText(strId);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable);
        super.onPause();
    }

    private Handler handler = new Handler();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            startActivity(new Intent("android.settings.NFC_SETTINGS"));
        }
    };

    /**
     * 检测工作,判断设备的NFC支持情况
     *
     * @return 返回true或false
     */
    protected Boolean ifNFCUse() {
        if (mNfcAdapter == null) {
            Toast.makeText(this, getResources().getString(R.string.nfc_erro2), Toast.LENGTH_SHORT).show();
            this.finish();
            return false;
        }
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, getResources().getString(R.string.nfc_erro), Toast.LENGTH_SHORT).show();
            handler.postDelayed(runnable, 3000);
            return false;
        }
        return true;
    }


}
