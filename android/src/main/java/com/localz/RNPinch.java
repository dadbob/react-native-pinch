package com.localz;

import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.facebook.internal.BundleJSONConverter;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import com.facebook.react.bridge.UnexpectedNativeTypeException;
import com.facebook.react.bridge.WritableMap;
import com.localz.pinch.models.HttpRequest;
import com.localz.pinch.models.HttpResponse;
import com.localz.pinch.utils.HttpUtil;
import com.localz.pinch.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class RNPinch extends ReactContextBaseJavaModule {

    private static final String OPT_METHOD_KEY = "method";
    private static final String OPT_HEADER_KEY = "headers";
    private static final String OPT_BODY_KEY = "body";
    private static final String OPT_SSL_PINNING_KEY = "sslPinning";
    private static final String OPT_TIMEOUT_KEY = "timeoutInterval";

    private HttpUtil httpUtil;
    private String packageName = null;
    private String displayName = null;
    private String version = null;
    private String versionCode = null;

    public RNPinch(ReactApplicationContext reactContext) {
        super(reactContext);
        httpUtil = new HttpUtil();
        try {
            PackageManager pManager = reactContext.getPackageManager();
            packageName = reactContext.getPackageName();
            PackageInfo pInfo = pManager.getPackageInfo(packageName, 0);
            ApplicationInfo aInfo = pManager.getApplicationInfo(packageName, 0);
            displayName = pManager.getApplicationLabel(aInfo).toString();
            version = pInfo.versionName;
            versionCode = String.valueOf(pInfo.versionCode);
        } catch (NameNotFoundException nnfe) {
            System.out.println("RNAppInfo: package name not found");
        }
    }

    @Override
    public String getName() {
        return "RNPinch";
    }

    @ReactMethod
    public void fetch(String endpoint, ReadableMap opts, Callback callback) {
        try {
            WritableMap response = Arguments.createMap();
            HttpRequest request = new HttpRequest(endpoint);

            if (opts.hasKey(OPT_BODY_KEY)) {
                request.body = opts.getString(OPT_BODY_KEY);
            }
            if (opts.hasKey(OPT_METHOD_KEY)) {
                request.method = opts.getString(OPT_METHOD_KEY);
            }
            if (opts.hasKey(OPT_HEADER_KEY)) {
                request.headers = JsonUtil.convertReadableMapToJson(opts.getMap(OPT_HEADER_KEY));
            }
            if (opts.hasKey(OPT_SSL_PINNING_KEY)) {
                request.certFilename = opts.getMap(OPT_SSL_PINNING_KEY).getString("cert");
            }
            if (opts.hasKey(OPT_TIMEOUT_KEY)) {
                request.timeout = opts.getInt(OPT_TIMEOUT_KEY);
            }

            HttpResponse httpResponse = httpUtil.sendHttpRequest(request);
            JSONObject jsonHeaders = new JSONObject(httpResponse.headers.toString());

            response.putInt("status", httpResponse.statusCode);
            response.putString("statusText", httpResponse.statusText);
            response.putString("bodyString", httpResponse.bodyString);
            response.putMap("headers", Arguments.fromBundle(BundleJSONConverter.convertToBundle(jsonHeaders)));

            callback.invoke(null, response);
        } catch(JSONException | IOException | UnexpectedNativeTypeException | KeyStoreException | CertificateException | KeyManagementException | NoSuchAlgorithmException e) {
            Log.e("RNPinch", "Error: " + e);
            e.printStackTrace();
            callback.invoke(e.toString(), null);
        }
    }
}
