'use strict';

window.mocha = {};

window.beforeEachHooks = [];
window.afterEachHooks = [];

window.currentSuite = {};
window.currentSpec = {};

window.setup = function(callback) {
    console.log('!!!!!!!!!!!!!!!!!!!!!!!!!!!');
    beforeEachHooks.push(callback);
};

window.teardown = function(callback) {
    console.log('!!!!!!!!!!!!!!!!!!!!!!!!!!!');
    afterEachHooks.push(callback);
};
console.log('!!!!!!!!!!!!!!!!!!!!!!!!!!!');
