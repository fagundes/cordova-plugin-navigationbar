"use strict";

(function () {
    var baselineStatusBarFullscreen;
    var lifecycleChecksEnabled = false;
    var pauseObserved = false;
    var rotationCheckPending = false;
    var rotationConfirmed = false;
    var visibilityCycles = 0;

    function finish(message) {
        document.getElementById("result").textContent = message;
        console.log(message);
    }

    function fail(message) {
        finish("NAVIGATIONBAR_TEST_FAIL: " + message);
    }

    window.addEventListener("unhandledrejection", function (event) {
        var reason = event.reason;
        fail(reason && reason.message ? reason.message : String(reason));
    });

    function assert(condition, message) {
        if (!condition) {
            throw new Error(message);
        }
    }

    function inspect(success) {
        NavigationBarInspector.getState(success, function (message) {
            fail("inspector error: " + message);
        });
    }

    function waitForState(assertState, success) {
        var attempts = 0;
        var lastError;
        var lastState;

        function retry() {
            attempts += 1;
            if (attempts >= 100) {
                var message = lastError
                    ? lastError.message
                    : "navigation bar state did not settle";
                fail(message + "; last state: " + JSON.stringify(lastState));
                return;
            }

            window.setTimeout(poll, 200);
        }

        function poll() {
            NavigationBarInspector.getState(function (state) {
                lastState = state;
                try {
                    assertState(state);
                } catch (error) {
                    lastError = error;
                    retry();
                    return;
                }

                success(state);
            }, function (message) {
                lastError = new Error("inspector error: " + message);
                retry();
            });
        }
        poll();
    }

    function afterUiChange(callback) {
        window.setTimeout(callback, 300);
    }

    function assertStatusBarUnchanged(state) {
        assert(state.statusBarFullscreen === baselineStatusBarFullscreen,
            "status bar fullscreen state changed");
    }

    function assertNavigationBarSize(size) {
        assert(size && typeof size === "object", "size() returned no result");
        assert(typeof size.width === "number" && size.width >= 0,
            "invalid navigation bar width");
        assert(typeof size.height === "number" && size.height >= 0,
            "invalid navigation bar height");
        assert(typeof size.widthInPixels === "number" && size.widthInPixels >= 0,
            "invalid navigation bar pixel width");
        assert(typeof size.heightInPixels === "number" && size.heightInPixels >= 0,
            "invalid navigation bar pixel height");
        assert(["bottom", "left", "right"].indexOf(size.position) !== -1,
            "invalid navigation bar position");
    }

    function readSize(success) {
        NavigationBar.size(function (size) {
            try {
                assertNavigationBarSize(size);
                success(size);
            } catch (error) {
                fail(error.message);
            }
        }, function (message) {
            fail("size error: " + message);
        });
    }

    function confirmRotation() {
        var orientation = Number(window.orientation);
        var isLandscape = Math.abs(orientation) === 90
            || window.screen.width > window.screen.height;

        if (rotationConfirmed || rotationCheckPending || !isLandscape) {
            return;
        }

        rotationCheckPending = true;
        inspect(function (state) {
            try {
                assertStatusBarUnchanged(state);
                rotationConfirmed = true;
                finish("NAVIGATIONBAR_TEST_ROTATED");
            } catch (error) {
                fail(error.message);
            }
        });
    }

    function pollForRotation() {
        if (!lifecycleChecksEnabled || rotationConfirmed) {
            return;
        }

        confirmRotation();
        window.setTimeout(pollForRotation, 250);
    }

    document.addEventListener("pause", function () {
        if (lifecycleChecksEnabled) {
            pauseObserved = true;
            console.log("NAVIGATIONBAR_TEST_PAUSED");
        }
    }, false);

    document.addEventListener("resume", function () {
        if (lifecycleChecksEnabled && pauseObserved) {
            afterUiChange(function () {
                inspect(function (state) {
                    try {
                        assert(rotationConfirmed,
                            "application resumed before rotation was confirmed");
                        assertStatusBarUnchanged(state);
                        finish("NAVIGATIONBAR_TEST_PASS");
                    } catch (error) {
                        fail(error.message);
                    }
                });
            });
        }
    }, false);

    window.addEventListener("orientationchange", function () {
        if (lifecycleChecksEnabled) {
            afterUiChange(function () {
                confirmRotation();
            });
        }
    }, false);

    window.addEventListener("resize", function () {
        if (lifecycleChecksEnabled) {
            confirmRotation();
        }
    }, false);

    function testShow() {
        waitForState(function (state) {
            assert(state.navigationBarHidden === true,
                "hide() did not hide navigation bar");
            assertStatusBarUnchanged(state);
        }, function () {
            NavigationBar.show();
            waitForState(function (shownState) {
                assert(shownState.navigationBarHidden === false,
                    "show() did not show navigation bar");
                assertStatusBarUnchanged(shownState);
            }, function () {
                visibilityCycles += 1;
                if (visibilityCycles < 2) {
                    NavigationBar.hide();
                    testShow();
                    return;
                }

                document.getElementById("keyboard-target").focus();
                afterUiChange(function () {
                    inspect(function (keyboardState) {
                        try {
                            assertStatusBarUnchanged(keyboardState);
                            lifecycleChecksEnabled = true;
                            finish("NAVIGATIONBAR_TEST_READY");
                            pollForRotation();
                        } catch (error) {
                            fail(error.message);
                        }
                    });
                });
            });
        });
    }

    function testHide() {
        waitForState(function (state) {
            assertStatusBarUnchanged(state);
            if (state.sdk >= 26) {
                assert(state.lightNavigationBar === true,
                    "light navigation bar flag was not set");
            }
        }, function () {
            NavigationBar.hide();
            readSize(testShow);
        });
    }

    function testColor() {
        waitForState(function (state) {
            assertStatusBarUnchanged(state);
            if (state.sdk >= 21 && state.sdk < 35) {
                assert((state.navigationBarColor >>> 0) === 0xff112233,
                    "navigation bar color was not applied");
            } else if (state.sdk < 21) {
                assert(state.navigationBarColor === null,
                    "navigation bar color should be unavailable before API 21");
            }
        }, function (state) {
            readSize(function () {
                if (state.sdk >= 30) {
                    NavigationBar.backgroundColorByHexString("#000000", false, true);
                    waitForState(function (transparentState) {
                        assertStatusBarUnchanged(transparentState);
                        assert((transparentState.navigationBarColor >>> 0) === 0,
                            "transparent navigation bar was not applied");
                        assert(transparentState.navigationBarContrastEnforced === false,
                            "navigation bar contrast remained enforced");
                    }, function () {
                        NavigationBar.backgroundColorByHexString("#fff", true, false);
                        testHide();
                    });
                    return;
                }

                NavigationBar.backgroundColorByHexString("#fff", true);
                testHide();
            });
        });
    }

    document.addEventListener("deviceready", function () {
        waitForState(function (state) {
            assert(state.windowHasFocus === true,
                "application window did not gain focus");
        }, function (initialState) {
            try {
                baselineStatusBarFullscreen = initialState.statusBarFullscreen;
                assert(initialState.navigationBarHidden === false,
                    "navigation bar was unexpectedly hidden initially");
                if (initialState.sdk >= 21 && initialState.sdk < 35) {
                    assert((initialState.navigationBarColor >>> 0) === 0xff123456,
                        "initial NavigationBarBackgroundColor was not applied");
                }
                if (initialState.sdk >= 26) {
                    assert(initialState.lightNavigationBar === false,
                        "initial dark icon preference was not applied");
                }

                NavigationBar.backgroundColorByHexString("#123", false);
                NavigationBar.backgroundColorByHexString("invalid", false);
                NavigationBar.backgroundColorByName("not-a-color", false);
                testColor();
            } catch (error) {
                fail(error.message);
            }
        });
    }, false);
}());
