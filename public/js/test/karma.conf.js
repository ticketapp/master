module.exports = function(config) {
    config.set({
        basePath: '..',
        frameworks: ['jasmine'],
        files: ['test/unit/**/*.spec.js']
    });
};