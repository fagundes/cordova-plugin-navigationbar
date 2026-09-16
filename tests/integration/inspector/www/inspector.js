"use strict";

var exec = require("cordova/exec");

module.exports = {
    getState: function (success, failure) {
        exec(success, failure, "NavigationBarInspector", "getState", []);
    }
};
