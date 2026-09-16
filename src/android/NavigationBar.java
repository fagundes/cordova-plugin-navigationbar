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

    @Override
    public void initialize(
            final CordovaInterface cordova,
            CordovaWebView webView) {
        LOG.v(TAG, "NavigationBar: initialization");
        super.initialize(cordova, webView);

        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Apply the initial navigation bar style from config.xml.
                    // These preferences affect only the navigation bar;
                    // the status bar is left unchanged.
                    setNavigationBarBackgroundColor(
                            preferences.getString(
                                    "NavigationBarBackgroundColor",
                                    "#000000"),
                            preferences.getBoolean(
                                    "NavigationBarLight",
                                    false),
                            preferences.getBoolean(
                                    "NavigationBarTransparent",
                                    false));
                } catch (IllegalArgumentException exception) {
                    LOG.e(
                            TAG,
                            "Invalid NavigationBarBackgroundColor preference");
                }
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
        final Activity activity = cordova.getActivity();
        final Window window = activity.getWindow();

        if ("_ready".equals(action)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean navigationBarVisible = (window.getDecorView().getSystemUiVisibility()
                            & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                    callbackContext.sendPluginResult(
                            new PluginResult(PluginResult.Status.OK, navigationBarVisible));
                }
            });
            return true;
        }

        if ("show".equals(action)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showNavigationBar(window);
                    callbackContext.success();
                }
            });
            return true;
        }

        if ("hide".equals(action)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideNavigationBar(window);
                    callbackContext.success();
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
            final String color;
            final boolean lightNavigationBar;
            final boolean transparentNavigationBar;

            try {
                color = args.getString(0);
                lightNavigationBar = args.optBoolean(1, false);
                transparentNavigationBar = args.optBoolean(2, false);
            } catch (JSONException exception) {
                LOG.e(TAG, "Invalid navigation bar color arguments");
                callbackContext.success();
                return true;
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setNavigationBarBackgroundColor(
                                color,
                                lightNavigationBar,
                                transparentNavigationBar);
                    } catch (IllegalArgumentException exception) {
                        LOG.e(TAG, "Invalid navigation bar color: " + color);
                    }

                    callbackContext.success();
                }
            });

            return true;
        }

        return false;
    }
  
    public int pxToDp(int px) {
        float scaleRatio = cordova.getActivity()
                .getResources()
                .getDisplayMetrics()
                .density;
        return Math.round(px / scaleRatio);
    }

    public Map getNavigationBarSize(Context context) {
        int width = 0;
        int height = 0;
        String position = "bottom";

        if (Build.VERSION.SDK_INT >= 30) {
            WindowManager windowManager =
                    (WindowManager) context.getSystemService(
                            Context.WINDOW_SERVICE);
            WindowMetrics windowMetrics =
                    windowManager.getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets()
                    .getInsetsIgnoringVisibility(
                            WindowInsets.Type.navigationBars());

            width = insets.left == 0 && insets.right == 0
                    ? windowMetrics.getBounds().width()
                    : (insets.left > 0 ? insets.left : insets.right);

            height = insets.top == 0 && insets.bottom == 0
                    ? windowMetrics.getBounds().height()
                    : (insets.top > 0 ? insets.top : insets.bottom);

            if (insets.left > 0) {
                position = "left";
            } else if (insets.right > 0) {
                position = "right";
            }

            if (insets.left == 0
                    && insets.right == 0
                    && insets.top == 0
                    && insets.bottom == 0) {
                width = 0;
                height = 0;
                position = "bottom";
            }
        } else {
            Point appUsableSize = getAppUsableScreenSize(context);
            Point realScreenSize = getRealScreenSize(context);

            if (appUsableSize.x < realScreenSize.x) {
                position = "right";
                width = realScreenSize.x - appUsableSize.x;
                height = appUsableSize.y;
            }

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
        WindowManager windowManager =
                (WindowManager) context.getSystemService(
                        Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public Point getRealScreenSize(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(
                        Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer) Display.class
                        .getMethod("getRawWidth")
                        .invoke(display);
                size.y = (Integer) Display.class
                        .getMethod("getRawHeight")
                        .invoke(display);
            } catch (IllegalAccessException exception) {
                // Leave the unavailable dimensions as zero.
            } catch (InvocationTargetException exception) {
                // Leave the unavailable dimensions as zero.
            } catch (NoSuchMethodException exception) {
                // Leave the unavailable dimensions as zero.
            }
        }

        return size;
    }

    private void showNavigationBar(final Window window) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }

        View decorView = window.getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void hideNavigationBar(final Window window) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }

        View decorView = window.getDecorView();
        int uiOptions = decorView.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void setNavigationBarBackgroundColor(
            final String colorPref,
            final boolean lightNavigationBar,
            final boolean transparentNavigationBar) {
        int color = Color.parseColor(normalizeColor(colorPref));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        final Window window = cordova.getActivity().getWindow();
        final View decorView = window.getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();

        window.clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            uiOptions &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }

        boolean transparent =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        && transparentNavigationBar;

        if (transparent) {
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        } else {
            uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }

        decorView.setSystemUiVisibility(uiOptions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(window, decorView);
            if (controller != null) {
                controller.setAppearanceLightNavigationBars(
                        lightNavigationBar);
            }
        }

        window.setNavigationBarColor(
                transparent ? Color.TRANSPARENT : color);
    }

    private String normalizeColor(final String colorPref) {
        if (colorPref == null) {
            throw new IllegalArgumentException("Color must not be null");
        }

        String normalized = colorPref.trim();
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }

        if (!normalized.matches(
                "^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|"
                        + "[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal color");
        }

        if (normalized.length() == 4 || normalized.length() == 5) {
            StringBuilder expanded = new StringBuilder("#");
            for (int index = 1;
                    index < normalized.length();
                    index += 1) {
                expanded.append(normalized.charAt(index));
                expanded.append(normalized.charAt(index));
            }
            normalized = expanded.toString();
        }

        return normalized;
    }
}
