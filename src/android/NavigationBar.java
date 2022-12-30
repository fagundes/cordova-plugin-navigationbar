/*
 * Copyright (c) 2016 by Vinicius Fagundes. All rights reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apache License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://opensource.org/licenses/Apache-2.0/ and read it before using this
 * file.
 *
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 *
 */

package com.viniciusfagundes.cordova.plugin.navigationbar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Insets;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.WindowInsets;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


public class NavigationBar extends CordovaPlugin {
    private static final String TAG = "NavigationBar";

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "NavigationBar: initialization");
        super.initialize(cordova, webView);

        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Clear flag FLAG_FORCE_NOT_FULLSCREEN which is set initially
                // by the Cordova.
                Window window = cordova.getActivity().getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

                // Read 'NavigationBarBackgroundColor' and 'NavigationBarLight' from config.xml, default is #000000.
                setNavigationBarBackgroundColor(preferences.getString("NavigationBarBackgroundColor", "#000000"), preferences.getBoolean("NavigationBarLight", false), preferences.getBoolean("NavigationBarTransparent", false));
            }
        });
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return True if the action was valid, false otherwise.
     */
    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        LOG.v(TAG, "Executing action: " + action);
        final Activity activity = this.cordova.getActivity();
        final Window window = activity.getWindow();

        if ("_ready".equals(action)) {
            boolean navigationBarVisible = (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0;
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, navigationBarVisible));
            return true;
        }

        if ("show".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // SYSTEM_UI_FLAG_FULLSCREEN is available since JellyBean, but we
                    // use KitKat here to be aligned with "Fullscreen"  preference
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int uiOptions = window.getDecorView().getSystemUiVisibility();
                        uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                        uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                        window.getDecorView().setSystemUiVisibility(uiOptions);

                        window.getDecorView().setOnFocusChangeListener(null);
                        window.getDecorView().setOnSystemUiVisibilityChangeListener(null);
                    }

                    // CB-11197 We still need to update LayoutParams to force navigation bar
                    // to be hidden when entering e.g. text fields
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });
            return true;
        }

        if ("hide".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // SYSTEM_UI_FLAG_FULLSCREEN is available since JellyBean, but we
                    // use KitKat here to be aligned with "Fullscreen"  preference
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        final int uiOptions = window.getDecorView().getSystemUiVisibility()
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                        window.getDecorView().setSystemUiVisibility(uiOptions);

                        window.getDecorView().setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (hasFocus) {
                                    window.getDecorView().setSystemUiVisibility(uiOptions);
                                }
                            }
                        });

                        window.getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                window.getDecorView().setSystemUiVisibility(uiOptions);
                            }
                        });
                    }

                    // CB-11197 We still need to update LayoutParams to force navigation bar
                    // to be hidden when entering e.g. text fields
                    //window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });
            return true;
        }

        if ("size".equals(action)) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Map size = getNavigationBarSize(cordova.getActivity().getApplicationContext());
                    int width = (Integer) size.get("width");
                    int height = (Integer) size.get("height");
                    String position = (String) size.get("position");
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("width", pxToDp(width));
                        obj.put("height", pxToDp(height));
                        obj.put("widthInPixels", width);
                        obj.put("heightInPixels", height);
                        obj.put("position", position);
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, obj));
                    } catch (JSONException e) {
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
                    }
                }
            });
            return true;
        }

        if ("backgroundColorByHexString".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setNavigationBarBackgroundColor(args.getString(0), args.getBoolean(1), args.getBoolean(2));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                }
            });
            return true;
        }

        return false;
    }

    public int pxToDp(int px) {
        float scaleRatio = cordova.getActivity().getResources().getDisplayMetrics().density;
        int dp = Math.round(px / scaleRatio);
        return dp;
    }

    public Map getNavigationBarSize(Context context) {
        int width = 0;
        int height = 0;
        String position = "bottom";
        if(Build.VERSION.SDK_INT >= 30) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars());
            width = insets.left == 0 && insets.right == 0 ? windowMetrics.getBounds().width() : (insets.left > 0 ? insets.left : insets.right);
            height = insets.top == 0 && insets.bottom == 0 ? windowMetrics.getBounds().height() : (insets.top > 0 ? insets.top : insets.bottom);
            if(insets.left > 0)
                position = "left";
            else if(insets.right > 0)
                position = "right";
        } else {
            Point appUsableSize = getAppUsableScreenSize(context);
            Point realScreenSize = getRealScreenSize(context);
            // navigation bar on the side
            if (appUsableSize.x < realScreenSize.x) {
                position = "right";
                width = realScreenSize.x - appUsableSize.x;
                height = appUsableSize.y;
            }
            // navigation bar at the bottom
            if (appUsableSize.y < realScreenSize.y) {
                width = appUsableSize.x;
                height = realScreenSize.y - appUsableSize.y;
            }
        }
        final Map<String, Object> size = new HashMap<>();
        size.put("width", width);
        size.put("height", height);
        size.put("position", position);
        return size;
    }

    public Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {} catch (InvocationTargetException e) {} catch (NoSuchMethodException e) {}
        }

        return size;
    }

    private void setNavigationBarBackgroundColor(final String colorPref, Boolean lightNavigationBar, Boolean transparentNavigationBar) {

        lightNavigationBar = lightNavigationBar != null && lightNavigationBar;
        transparentNavigationBar = transparentNavigationBar != null && transparentNavigationBar;

        if (Build.VERSION.SDK_INT >= 21) {
            if (colorPref != null && !colorPref.isEmpty()) {
                final Window window = cordova.getActivity().getWindow();
                final View decorView = window.getDecorView();
                int uiOptions = decorView.getSystemUiVisibility();

                // 0x80000000 FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                // 0x00000010 SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                // 0x00000200 FLAG_LAYOUT_NO_LIMITS

                uiOptions = uiOptions | 0x80000000;

                if(Build.VERSION.SDK_INT >= 26) {
                    WindowInsetsControllerCompat windowInsetsControllerCompat = WindowCompat.getInsetsController(window, decorView);

                    if(lightNavigationBar)
                        windowInsetsControllerCompat.setAppearanceLightNavigationBars(true);
                    else
                        windowInsetsControllerCompat.setAppearanceLightNavigationBars(false);
                } else {
                    uiOptions = uiOptions & ~0x00000010;
                }

                if(Build.VERSION.SDK_INT >= 30 && transparentNavigationBar) 
                    uiOptions = uiOptions | 0x00000200; // window.addFlags(0x00000200);
                else
                    uiOptions = uiOptions & ~0x00000200; // window.clearFlags(0x00000200);

                decorView.setSystemUiVisibility(uiOptions);

                try {
                    window.setNavigationBarColor(Build.VERSION.SDK_INT >= 30 && transparentNavigationBar ? Color.TRANSPARENT : Color.parseColor(colorPref));
                } catch (IllegalArgumentException ignore) {
                    LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
                } catch (Exception ignore) {
                    // this should not happen, only in case Android removes this method in a version > 21
                    LOG.w(TAG, "Method window.setNavigationBarColor not found for SDK level " + Build.VERSION.SDK_INT);
                }
            }
        }
    }
}
