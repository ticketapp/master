'use strict';
describe('Factory: DetectPath', function () {
    var DetectPath, $rootScope, $location;

    beforeEach(module('claudeApp'));
    beforeEach(inject(function (_DetectPath_, _$rootScope_, _$location_) {
        $location = _$location_;
        spyOn($location, 'path').and.returnValue('/');
        DetectPath = _DetectPath_;
        $rootScope = _$rootScope_;
    }));

    it('should return pathName', function () {
        expect($rootScope.path).toBe(false);
    });
});