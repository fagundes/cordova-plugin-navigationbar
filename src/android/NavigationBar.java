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
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

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
                try {
                    // Read the preferences from config.xml. These settings affect
                    // the navigation bar only; the status bar is deliberately left alone.
                    setNavigationBarBackgroundColor(
                            preferences.getString("NavigationBarBackgroundColor", "#000000"),
                            preferences.getBoolean("NavigationBarLight", false));
                } catch (IllegalArgumentException exception) {
                    LOG.e(TAG, "Invalid NavigationBarBackgroundColor preference");
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

        if ("backgroundColorByHexString".equals(action)) {
            final String color;
            final boolean lightNavigationBar;

            try {
                color = args.getString(0);
                lightNavigationBar = args.getBoolean(1);
            } catch (JSONException exception) {
                LOG.e(TAG, "Invalid color arguments");
                callbackContext.success();
                return true;
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setNavigationBarBackgroundColor(color, lightNavigationBar);
                        callbackContext.success();
                    } catch (IllegalArgumentException exception) {
                        LOG.e(TAG, "Invalid navigation bar color: " + color);
                        callbackContext.success();
                    }
                }
            });
            return true;
        }

        return false;
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
            final String colorPref, final boolean lightNavigationBar) {
        // Parse on every API level so invalid configuration is reported consistently.
        int color = Color.parseColor(normalizeColor(colorPref));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        final Window window = cordova.getActivity().getWindow();
        final View decorView = window.getDecorView();
        final WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(decorView);
        int uiOptions = decorView.getSystemUiVisibility();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && lightNavigationBar) {
            uiOptions |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            uiOptions &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }

        decorView.setSystemUiVisibility(uiOptions);

        if (controller != null) {
            controller.setAppearanceLightNavigationBars(lightNavigationBar);
        }

        window.setNavigationBarColor(color);
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
                "^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")) {
            throw new IllegalArgumentException("Invalid hexadecimal color");
        }

        if (normalized.length() == 4 || normalized.length() == 5) {
            StringBuilder expanded = new StringBuilder("#");
            for (int index = 1; index < normalized.length(); index += 1) {
                expanded.append(normalized.charAt(index));
                expanded.append(normalized.charAt(index));
            }
            normalized = expanded.toString();
        }

        return normalized;
    }
}
