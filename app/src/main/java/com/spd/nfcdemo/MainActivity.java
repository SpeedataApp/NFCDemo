package com.spd.nfcdemo;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.spd.nfcdemo.iso15693.NfcVUtil;
import com.spd.nfcdemo.mifare.MifareBlock;
import com.spd.nfcdemo.mifare.MifareClassCard;
import com.spd.nfcdemo.mifare.MifareSector;
import com.spd.nfcdemo.utils.Converter;
import com.spd.nfcdemo.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author zzc
 * @date 2019/12/9
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private TextView tvTech;
    private TextView tvId;
    private TextView tvWrite;

    ListView list = null;
    ArrayList<String> blockData;
    ListAdapter adapter;

    MifareClassic mfc;
    MifareClassCard mifareClassCard = null;

    private NfcV mNfcV;
    // NFC parts
    private static NfcAdapter mAdapter;

    private boolean isNFC;
    private static PendingIntent mPendingIntent;
    private static IntentFilter[] mFilters;
    private static String[][] mTechLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvTech = findViewById(R.id.tv_nfc_tech);
        tvId = findViewById(R.id.tv_nfc_id);
        tvWrite = findViewById(R.id.tv_nfc_write);

        list = findViewById(R.id.list);

        // Register the onClick listener with the implementation above
        isNFC = getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_NFC);

        tvWrite.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // NFC适配器，所有的关于NFC的操作从该适配器进行
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!ifNFCUse()) {
            Log.d("zzc", "mNfcAdapter: false");
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        handler.removeCallbacks(runnable);
        initNFC();
        if (isNFC && mAdapter.isEnabled()) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    boolean isMafire = false;
    boolean is15693 = false;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }

        //获取标签ID
        byte[] byteId = tag.getId();
        String strId = StringUtils.byteArrayToString(byteId);
        tvId.setText(strId);

        isMafire = false;
        is15693 = false;
        //获取标签类型
        String[] techList = tag.getTechList();
        tvTech.setText("");
        for (String tech : techList) {
            tvTech.append(tech + "\n");
            if ("android.nfc.tech.MifareClassic".equals(tech)) {
                isMafire = true;
            }
            if ("android.nfc.tech.NfcV".equals(tech)) {
                is15693 = true;
            }
        }

        clearFields();

        if (isMafire) {
            mfc = MifareClassic.get(tag);
            if (mfc != null) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            try {
                                mfc.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                mfc.connect();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            boolean auth = false;

                            final int secCount = mfc.getSectorCount();
                            mifareClassCard = new MifareClassCard(secCount);
                            int bCount;
                            int bIndex;
                            for (int j = 0; j < secCount; j++) {
                                MifareSector mifareSector = new MifareSector();
                                mifareSector.sectorIndex = j;
                                try {
                                    auth = mfc.authenticateSectorWithKeyA(j,
                                            MifareClassic.KEY_DEFAULT);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                mifareSector.authorized = auth;
                                if (auth) {
                                    bCount = mfc.getBlockCountInSector(j);
                                    bCount = Math.min(bCount,
                                            MifareSector.BLOCKCOUNT);
                                    bIndex = mfc.sectorToBlock(j);
                                    for (int i = 0; i < bCount; i++) {

                                        // 6.3) Read the block
                                        byte[] data = new byte[0];
                                        try {
                                            data = mfc.readBlock(bIndex);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        MifareBlock mifareBlock = new MifareBlock(
                                                data);
                                        mifareBlock.blockIndex = bIndex;
                                        bIndex++;
                                        mifareSector.blocks[i] = mifareBlock;

                                    }
                                    mifareClassCard.setSector(
                                            mifareSector.sectorIndex,
                                            mifareSector);
                                }

                            }
                            blockData = new ArrayList<>();
                            int blockIndex = 0;
                            for (int i = 0; i < secCount; i++) {

                                MifareSector mifareSector = mifareClassCard
                                        .getSector(i);
                                for (int j = 0; j < MifareSector.BLOCKCOUNT; j++) {
                                    MifareBlock mifareBlock = mifareSector.blocks[j];
                                    byte[] data = mifareBlock.getData();
                                    blockData.add("Block "
                                            + blockIndex++
                                            + " : "
                                            + Converter.getHexString(data,
                                            data.length));
                                }
                            }
                            String[] contents = new String[blockData.size()];
                            Message msg = new Message();
                            msg.obj = contents;
                            handler2.sendMessage(msg);
                            try {
                                mfc.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            handler.postDelayed(() ->
                                    Toast.makeText(MainActivity.this, "Failed reading 0", Toast.LENGTH_SHORT).show(), 0);
                        }
                    }
                }.start();
            }
        } else if (is15693) {
            mNfcV = NfcV.get(tag);
            if (mNfcV != null) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            try {
                                mNfcV.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                mNfcV.connect();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            NfcVUtil mNfcVutil = new NfcVUtil(mNfcV);
                            //读卡28个块内容
                            blockData = new ArrayList<>();
                            int blockIndex = 0;
                            for (int j = 0; j < 28; j++) {

                                String data = mNfcVutil.readOneBlock(j);
                                blockData.add("Block "
                                        + blockIndex++
                                        + " : "
                                        + data);
                            }
                            String[] contents = new String[blockData.size()];
                            Message msg = new Message();
                            msg.obj = contents;
                            handler2.sendMessage(msg);
                            try {
                                mNfcV.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            handler.postDelayed(() ->
                                    Toast.makeText(MainActivity.this, "Failed reading 0", Toast.LENGTH_SHORT).show(), 0);
                        }
                    }
                }.start();
            }
        }

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

    private void clearFields() {
        if (blockData != null) {
            blockData.clear();
            list.setAdapter(null);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String[] contents = (String[]) msg.obj;
            blockData.toArray(contents);
            adapter = new ArrayAdapter<>(MainActivity.this,
                    android.R.layout.simple_list_item_1, contents);
            list.setAdapter(adapter);
        }

    };

    private void initNFC() {
        if (isNFC) {
            mAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mAdapter.isEnabled()) {
                // Create a generic PendingIntent that will be deliver to this
                // activity.
                // The NFC stack
                // will fill in the intent with the details of the discovered
                // tag before
                // delivering to
                // this activity.
                mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(
                        this, getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                // Setup an intent filter for all MIME based dispatches
                IntentFilter ndef = new IntentFilter(
                        NfcAdapter.ACTION_TECH_DISCOVERED);

                try {
                    ndef.addDataType("*/*");
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    throw new RuntimeException("fail", e);
                }
                mFilters = new IntentFilter[]{ndef,};
                // Setup a tech list for all NfcF tags
                mTechLists = new String[][]{new String[]{MifareClassic.class
                        .getName()}};
                Intent intent = getIntent();
                resolveIntent(intent);
            } else {
                Toast.makeText(this, "settings", Toast.LENGTH_SHORT).show();
                new AlertDialog.Builder(this)
                        .setTitle("Settings")
                        // .setMessage("5")
                        .setNegativeButton("open",
                                (dialog, which) -> {
                                    Intent callGPSSettingIntent = new Intent(
                                            android.provider.Settings.ACTION_NFC_SETTINGS);
                                    startActivity(callGPSSettingIntent);
                                })
                        .setPositiveButton("cancel",
                                (dialog, which) -> {

                                }).create().show();
            }
        } else {
            Toast.makeText(this, "Please put the card in appointed field",
                    Toast.LENGTH_SHORT).show();
        }
    }

    void resolveIntent(Intent intent) {
        // 1) Parse the intent and get the action that triggered this intent
        String action = intent.getAction();
        // 2) Check if it was triggered by a tag discovered interruption.
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            // 3) Get an instance of the TAG from the NfcAdapter
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            // 4) Get an instance of the Mifare classic card from this TAG
            // intent
            mfc = MifareClassic.get(tagFromIntent);
            mNfcV = NfcV.get(tagFromIntent);
        }// End of method
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_nfc_write) {
            if (isMafire) {
                //mafire写卡
                if (mfc == null) {
                    return;
                }
                try {
                    mfc.connect();
                    boolean auth;
                    for (int i = 4; i < 14; i++) {
                        auth = mfc.authenticateSectorWithKeyA(i,
                                MifareClassic.KEY_DEFAULT);
                        if (auth) {
                            // the last block of the sector is used for KeyA and
                            // KeyB cannot be overwritted
                            mfc.writeBlock(4 * i, "1313838438000000".getBytes());
                        }
                    }
                    Toast.makeText(MainActivity.this,
                            "1313838438000000 write successfully",
                            Toast.LENGTH_SHORT).show();
                    mfc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        mfc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else if (is15693) {
                //15693写卡
                if (mNfcV == null) {
                    return;
                }
                try {
                    mNfcV.connect();
                    NfcVUtil mNfcVutil = new NfcVUtil(mNfcV);
                    for (int i = 10; i < 20; i++) {
                        mNfcVutil.writeBlock(i, new byte[]{1, 1, 1, 1});
                    }

                    Toast.makeText(MainActivity.this, "write success", Toast.LENGTH_SHORT)
                            .show();
                    mNfcV.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        mNfcV.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
