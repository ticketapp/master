'use strict';

describe('Controller: ConnectCtrl', function () {
    var $ctrl,
        $scope,
        $httpBackend,
        $localStorage;
    // load the controller's module
    beforeEach(module('claudeApp'));

    beforeEach(inject(function ($controller, _$rootScope_, _$localStorage_, _UserFactory_, _$httpBackend_) {
        $httpBackend = _$httpBackend_;
        $httpBackend.when('GET', '/users/tracksRemoved/')
            .respond([
                {name: 'aaa', trackId: 1},
                {name: 'bbb', trackId: 2},
                {name: 'ccc', trackId: 3}
            ]);

        $localStorage = _$localStorage_;

        $scope = _$rootScope_;
        $ctrl = $controller('connectCtrl', {
            $scope: $scope
        });
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it('should update signaled tracks', function () {
        var expectedSignaledTracks = [1, 2, 3];
        $localStorage.tracksSignaled = [1, 2];
        $scope.updateRemoveTracks();

        $httpBackend.flush();

        expect($localStorage.tracksSignaled).toEqual(expectedSignaledTracks);
    });

    it('should rate new signaled tracks', function () {
        var expectedSignaledTracks = [1, 2, 3];
        $localStorage.tracksSignaled = [1, 2, 3, 4];
        $scope.updateRemoveTracks();

        $httpBackend.flush();

        expect($localStorage.tracksSignaled).toEqual(expectedSignaledTracks);
    });
});