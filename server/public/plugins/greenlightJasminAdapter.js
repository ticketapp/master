'use strict';

window.mocha = {};

window.beforeEachHooks = [];
window.afterEachHooks = [];

window.currentSuite = {};
window.currentSpec = {};

window.setup = function(callback) {
    beforeEachHooks.push(callback);
};

window.teardown = function(callback) {
    afterEachHooks.push(callback);
};
