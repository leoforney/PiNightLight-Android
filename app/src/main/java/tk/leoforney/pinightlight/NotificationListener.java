package tk.leoforney.pinightlight;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {

    List<AppRecord> appRecords;

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client;

    @Override
    public IBinder onBind(Intent intent) {
        client = new OkHttpClient();
        Log.d(this.getClass().getName(), "Service Binded");
        appRecords = new ArrayList<>();
        appRecords.add(new AppRecord("Instagram", "com.instagram.android", "#e95950"));
        appRecords.add(new AppRecord("Snapchat", "com.snapchat.android", "#fffc00"));
        appRecords.add(new AppRecord("Messaging (Samsung)", "com.samsung.android.messaging", "#4c68d7"));
        appRecords.add(new AppRecord("Messaging (Google)", "com.google.android.apps.messaging", "#4c68d7"));
        appRecords.add(new AppRecord("Messaging (OnePlus)", "com.android.mms", "#4c68d7"));
        return super.onBind(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Implement what you want here
        String wifi = MainActivity.getCurrentSsid(getApplicationContext()).replaceAll("^\"|\"$", "");
        Log.d(this.getClass().getName(), wifi);
        if (wifi.equals("Cassibo")) {
            String packageName = sbn.getPackageName();
            AppRecord appRecord = matchAppRecord(packageName);
            if (appRecord != null) {
                Log.d(this.getClass().getName(), "Notification received from " + appRecord.name + " : " + appRecord.hex);
                RequestBody body = RequestBody.create(JSON, appRecord.hex);
                Request request = new Request.Builder()
                        .url(MainActivity.getRPIUrl(getApplicationContext()) + "/notification")
                        .addHeader("Token", "bGVvZm9ybmV5OmRhcnlsZW8x")
                        .post(body)
                        .build();

                String url = "https://api.lifx.com/v1/lights/id:d073d5348dac/effects/breathe?" +
                        "color=" + appRecord.hex + "&" +
                        "period=3&" +
                        "cycles=1&" +
                        "power_on=false";
                Log.d("NLPiNightLight", url);

                Request lifxRequest = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", String.format("Bearer %s", this.getResources().getString(R.string.lifx_token)))
                        .post(RequestBody.create(MediaType.parse("text"), ""))
                        .build();

                client.newCall(lifxRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseString = response.body().string();
                        Log.d("NLPiNightLight", responseString);

                    }
                });

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                    }
                });

            }
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Implement what you want here
    }

    private AppRecord matchAppRecord(String packageName) {
        AppRecord returnValue = null;
        for (AppRecord record : appRecords) {
            if (record.packageName.equals(packageName)) {
                returnValue = record;
            }
        }
        return returnValue;
    }
}
