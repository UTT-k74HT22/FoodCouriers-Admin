package com.utt.foodcouriers_admin.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.utt.foodcouriers_admin.utils.websocket.RealtimeListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Sử dụng để kết nối đến realtime channel.
 */
public class SupabaseRealtimeClient {

    private static final String TAG = "SupabaseRealtime";
    // Supabase Realtime is Phoenix-based. Heartbeats should be sent before the 30s timeout window.
    private static final int HEARTBEAT_INTERVAL_SECONDS = 25;
    private static final int RECONNECT_DELAY_SECONDS = 5;

    private static SupabaseRealtimeClient instance;

    private WebSocket webSocket;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Handler mainHandler;
    private final AtomicInteger refCounter = new AtomicInteger(0);

    // Map<channelId, listener>
    private final Map<String, RealtimeListener> channels = new HashMap<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatFuture;
    private boolean isConnected = false;
    private boolean shouldReconnect = true;
    private String accessToken;

    private SupabaseRealtimeClient() {
        httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket không timeout
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public static synchronized SupabaseRealtimeClient getInstance() {
        if (instance == null) {
            instance = new SupabaseRealtimeClient();
        }
        return instance;
    }

    public String getRealtimeWebsocketUrl() {
        return SupabaseConfig.getRealtimeWebsocketUrl();
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void disconnect() {
        shouldReconnect = false;
        isConnected = false;
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
            heartbeatFuture = null;
        }
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
    }

}
