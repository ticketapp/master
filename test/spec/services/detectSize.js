'use strict';
describe('Factory: DetectSize', function () {
    var DetectSize;
    beforeEach(module('claudeApp'));
    beforeEach(function () {

        inject(function ($injector) {
            DetectSize = $injector.get('DetectSize');
        });

    });

    it('should return windowSizeName', function () {
        expect(DetectSize).toBe(false);
    });
});