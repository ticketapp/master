'use strict';
describe('Factory: RefactorGeopoint', function () {
    var RefactorGeopoint;

    beforeEach(module('claudeApp'));
    beforeEach(inject(function (_RefactorGeopoint_) {
        RefactorGeopoint = _RefactorGeopoint_;
    }));

    it('should refactore websites', function () {
        var geoPoints = '(1.12121,2.112121)';
        var normalizedGeopoint = RefactorGeopoint.refactorGeopoint(geoPoints);

        var expectedGeopoints = '1.12121, 2.112121';

        expect(normalizedGeopoint).toEqual(expectedGeopoints);
    });
});