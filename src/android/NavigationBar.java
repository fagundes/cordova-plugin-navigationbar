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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowMetrics;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

public class NavigationBar extends CordovaPlugin {
    private static final String TAG = "NavigationBar";

    private boolean lightNavigationBar;
    private boolean transparentNavigationBar;
    private boolean visibilityManaged;
    private boolean navigationBarHidden;
    private int navigationBarColor = Color.BLACK;
    private View decorView;
    private ViewTreeObserver.OnWindowFocusChangeListener focusChangeListener;

    @Override
    protected void pluginInitialize() {
        final Activity activity = cordova.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Window window = activity.getWindow();
                decorView = window.getDecorView();
                installFocusListener();

                try {
                    setNavigationBarBackgroundColor(
                            preferences.getString("NavigationBarBackgroundColor", "#000000"),
                            preferences.getBoolean("NavigationBarLight", false),
                            preferences.getBoolean("NavigationBarTransparent", false));
                } catch (IllegalArgumentException exception) {
                    LOG.e(TAG, "Invalid NavigationBarBackgroundColor preference");
                }
            }
        });
    }

    @Override
    public boolean execute(
            final String action,
            final CordovaArgs args,
            final CallbackContext callbackContext) throws JSONException {
        LOG.v(TAG, "Executing action: " + action);
        final Activity activity = cordova.getActivity();
        final Window window = activity.getWindow();

        if ("_ready".equals(action)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callbackContext.sendPluginResult(new PluginResult(
                            PluginResult.Status.OK,
                            isNavigationBarVisible(window)));
                }
            });
            return true;
        }

        if ("show".equals(action)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    visibilityManaged = true;
                    navigationBarHidden = false;
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
                    visibilityManaged = true;
                    navigationBarHidden = true;
                    hideNavigationBar(window);
                    callbackContext.success();
                }
            });
            return true;
        }

        if ("backgroundColorByHexString".equals(action)) {
            final String color;
            final boolean light;
            final boolean transparent;

            try {
                color = args.getString(0);
                light = args.optBoolean(1);
                transparent = args.optBoolean(2);
            } catch (JSONException exception) {
                LOG.e(TAG, "Invalid navigation bar color arguments");
                callbackContext.success();
                return true;
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setNavigationBarBackgroundColor(color, light, transparent);
                    } catch (IllegalArgumentException exception) {
                        LOG.e(TAG, "Invalid navigation bar color: " + color);
                    }
                    callbackContext.success();
                }
            });
            return true;
        }

        if ("size".equals(action)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        callbackContext.success(getNavigationBarSize(window));
                    } catch (JSONException exception) {
                        LOG.e(TAG, "Unable to serialize navigation bar insets", exception);
                        callbackContext.error("Unable to read navigation bar insets");
                    } catch (RuntimeException exception) {
                        LOG.e(TAG, "Unable to read navigation bar insets", exception);
                        callbackContext.error("Unable to read navigation bar insets");
                    }
                }
            });
            return true;
        }

        return false;
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        reapplyRequestedState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reapplyRequestedState();
    }

    @Override
    public void onDestroy() {
        final View view = decorView;
        final ViewTreeObserver.OnWindowFocusChangeListener listener = focusChangeListener;
        if (view != null && listener != null) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewTreeObserver observer = view.getViewTreeObserver();
                    if (observer.isAlive()) {
                        observer.removeOnWindowFocusChangeListener(listener);
                    }
                }
            });
        }
        decorView = null;
        focusChangeListener = null;
        super.onDestroy();
    }

    private void installFocusListener() {
        if (decorView == null || focusChangeListener != null) {
            return;
        }

        focusChangeListener = new ViewTreeObserver.OnWindowFocusChangeListener() {
            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                if (hasFocus) {
                    reapplyRequestedState();
                }
            }
        };
        decorView.getViewTreeObserver().addOnWindowFocusChangeListener(focusChangeListener);
    }

    private void reapplyRequestedState() {
        if (decorView == null) {
            return;
        }

        final Activity activity = cordova.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Window window = activity.getWindow();
                applyNavigationBarStyle(window);
                if (visibilityManaged) {
                    if (navigationBarHidden) {
                        hideNavigationBar(window);
                    } else {
                        showNavigationBar(window);
                    }
                }
            }
        });
    }

    private boolean isNavigationBarVisible(final Window window) {
        View view = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets insets = view.getRootWindowInsets();
            if (insets != null) {
                return insets.isVisible(WindowInsets.Type.navigationBars());
            }
        }
        return (view.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
    }

    private void showNavigationBar(final Window window) {
        View view = window.getDecorView();
        int uiOptions = view.getSystemUiVisibility();
        uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (!transparentNavigationBar) {
            uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        view.setSystemUiVisibility(uiOptions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.navigationBars());
            }
        }
    }

    private void hideNavigationBar(final Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.navigationBars());
            }
            return;
        }

        View view = window.getDecorView();
        int uiOptions = view.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        view.setSystemUiVisibility(uiOptions);
    }

    private void setNavigationBarBackgroundColor(
            final String colorPref,
            final boolean light,
            final boolean transparent) {
        navigationBarColor = Color.parseColor(normalizeColor(colorPref));
        lightNavigationBar = light;
        transparentNavigationBar = transparent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;

        Window window = cordova.getActivity().getWindow();
        applyNavigationBarStyle(window);
        if (visibilityManaged && navigationBarHidden) {
            hideNavigationBar(window);
        }
    }

    private void applyNavigationBarStyle(final Window window) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        View view = window.getDecorView();
        int uiOptions = view.getSystemUiVisibility();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (lightNavigationBar) {
                uiOptions |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                uiOptions &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }

        if (transparentNavigationBar) {
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        } else if (!navigationBarHidden) {
            uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }

        view.setSystemUiVisibility(uiOptions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        lightNavigationBar
                                ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                                : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(!transparentNavigationBar);
        }
        window.setNavigationBarColor(
                transparentNavigationBar ? Color.TRANSPARENT : navigationBarColor);
    }

    private JSONObject getNavigationBarSize(final Window window) throws JSONException {
        int widthInPixels = 0;
        int heightInPixels = 0;
        String position = "bottom";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = cordova.getActivity().getWindowManager()
                    .getCurrentWindowMetrics();
            Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars());
            Rect bounds = metrics.getBounds();

            if (insets.left > 0) {
                widthInPixels = insets.left;
                heightInPixels = bounds.height();
                position = "left";
            } else if (insets.right > 0) {
                widthInPixels = insets.right;
                heightInPixels = bounds.height();
                position = "right";
            } else if (insets.bottom > 0) {
                widthInPixels = bounds.width();
                heightInPixels = insets.bottom;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowInsets insets = window.getDecorView().getRootWindowInsets();
            if (insets != null) {
                int left = insets.getStableInsetLeft();
                int right = insets.getStableInsetRight();
                int bottom = insets.getStableInsetBottom();
                Point realSize = getRealScreenSize(window);

                if (left > 0) {
                    widthInPixels = left;
                    heightInPixels = realSize.y;
                    position = "left";
                } else if (right > 0) {
                    widthInPixels = right;
                    heightInPixels = realSize.y;
                    position = "right";
                } else if (bottom > 0) {
                    widthInPixels = realSize.x;
                    heightInPixels = bottom;
                }
            }
        } else {
            Point usableSize = getUsableScreenSize(window);
            Point realSize = getRealScreenSize(window);
            if (usableSize.x < realSize.x) {
                widthInPixels = realSize.x - usableSize.x;
                heightInPixels = usableSize.y;
                position = "right";
            } else if (usableSize.y < realSize.y) {
                widthInPixels = usableSize.x;
                heightInPixels = realSize.y - usableSize.y;
            }
        }

        float density = cordova.getActivity().getResources()
                .getDisplayMetrics().density;
        JSONObject size = new JSONObject();
        size.put("width", Math.round(widthInPixels / density));
        size.put("height", Math.round(heightInPixels / density));
        size.put("widthInPixels", widthInPixels);
        size.put("heightInPixels", heightInPixels);
        size.put("position", position);
        return size;
    }

    @SuppressWarnings("deprecation")
    private Point getUsableScreenSize(final Window window) {
        Display display = window.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    @SuppressWarnings("deprecation")
    private Point getRealScreenSize(final Window window) {
        Display display = window.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return size;
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
