package tk.leoforney.pinightlight;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.pavelsikun.vintagechroma.ChromaDialog;
import com.pavelsikun.vintagechroma.IndicatorMode;
import com.pavelsikun.vintagechroma.OnColorSelectedListener;
import com.pavelsikun.vintagechroma.colormode.ColorMode;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button colorPickerButton, lifxButton;
    EditText ipEditText;
    SeekBar brightnessBar;
    TextView brightnessCurrent;
    TextView colorView;
    final static String TAG = MainActivity.class.getName();
    OkHttpClient client;
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private AlertDialog enableNotificationListenerAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                1
        );

        client = new OkHttpClient();

        if (!isNotificationServiceEnabled()) {
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }

        colorView = findViewById(R.id.colorView);
        lifxButton = findViewById(R.id.lifx_button);
        lifxButton.setOnClickListener(this);

        client.newCall(new Request.Builder()
                .url(getRPIUrl() + "/color")
                .get()
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String brightnessAsString = response.body().string();
                final int madeUpColor = Color.parseColor("#" + brightnessAsString);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        colorView.setBackgroundColor(madeUpColor);
                    }
                });
            }
        });

        colorPickerButton = findViewById(R.id.colorPickerButton);
        colorPickerButton.setOnClickListener(this);

        brightnessCurrent = findViewById(R.id.brightnessCurrent);
        brightnessBar = findViewById(R.id.brightnessSeekbar);
        client.newCall(new Request.Builder()
                .url(getRPIUrl() + "/brightness")
                .get()
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String brightnessAsString = response.body().string();
                final int brightness = Math.round(Float.valueOf(brightnessAsString));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        brightnessBar.setProgress(brightness);
                        brightnessCurrent.setText("Current brightness: " + brightness + "%");
                    }
                });

            }
        });
        brightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                brightnessCurrent.setText("Current brightness: " + i + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Request request = new Request.Builder()
                        .url(getRPIUrl() + "/brightness")
                        .post(RequestBody.create(MediaType.parse("text/plain"), String.valueOf(seekBar.getProgress())))
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseString = response.body().string();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make((findViewById(R.id.coordinatorLayout)), responseString, Snackbar.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
            }
        });

        ipEditText = findViewById(R.id.ipEditText);
        ipEditText.setText(getRPIUrl());
        ipEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                SharedPreferences pref = getApplicationContext().getSharedPreferences(this.getClass().getPackage().getName(), MODE_PRIVATE);
                pref.edit().putString("ip", charSequence.toString()).apply();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.lifx_button:
                Request request = new Request.Builder()
                        .url("https://api.lifx.com/v1/lights/id:d073d5348dac/effects/breathe?" +
                                "color=hue:" + "2" + "&" +
                                "period=3&" +
                                "cycles=1&" +
                                "power_on=false")
                        .addHeader("Authorization", String.format("Bearer %s", this.getResources().getString(R.string.lifx_token)))
                        .post(RequestBody.create(MediaType.parse("text"), ""))
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseString = response.body().string();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make((findViewById(R.id.coordinatorLayout)), responseString, Snackbar.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
                //Log.d("NLPiNightLight", lifxResponse.getBody());
                break;
            case R.id.colorPickerButton:
                new ChromaDialog.Builder()
                        .initialColor(Color.DKGRAY)
                        .colorMode(ColorMode.RGB)
                        .indicatorMode(IndicatorMode.HEX)
                        .onColorSelected(new OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int color) {
                                String hexColor = String.format("#%06X", (0xFFFFFF & color));
                                colorView.setBackgroundColor(color);
                                Log.d(TAG, String.valueOf(hexColor) + " @ " + getRPIUrl() + "/color");
                                Request request = new Request.Builder()
                                        .url(getRPIUrl() + "/color")
                                        .post(RequestBody.create(MediaType.parse("text/plain"), hexColor))
                                        .build();
                                client.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {

                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        final String responseString = response.body().string();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Snackbar.make((findViewById(R.id.coordinatorLayout)), responseString, Snackbar.LENGTH_SHORT).show();
                                            }
                                        });

                                    }
                                });
                            }
                        })
                        .create()
                        .show(getSupportFragmentManager(), "ChromaDialog");
                break;
        }
    }

    private String getRPIUrl() {
        return getRPIUrl(getApplicationContext());
    }

    public static String getRPIUrl(Context context) {
        SharedPreferences pref = context.getSharedPreferences(context.getClass().getPackage().getName(), MODE_PRIVATE);
        String totalName = pref.getString("ip", context.getResources().getString(R.string.ip));
        return totalName;
    }

    public static String getCurrentSsid(Context context) {
        String ssid = "";
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.isWifiEnabled()) {
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                    ssid = connectionInfo.getSSID();
                }
            }
        }
        return ssid;
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private AlertDialog buildNotificationServiceAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Enable Notification Service");
        alertDialogBuilder.setMessage("Enable it bitch");
        alertDialogBuilder.setPositiveButton("Soy pussy",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton("Fuck off",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return (alertDialogBuilder.create());
    }
}
