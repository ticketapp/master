'use strict';

describe('Controller: ArtistCtrl', function () {
    var $ctrl,
        $scope,
        $httpBackend,
        artistHandler,
        eventsHandler;
    // load the controller's module
    beforeEach(module('claudeApp'));

    beforeEach(inject(function ($controller, _$rootScope_, _$routeParams_, _ArtistsFactory_, _$httpBackend_) {
        $httpBackend = _$httpBackend_;
        // backend definition common for all tests
        artistHandler = $httpBackend.when('GET', '/artists/klklklkl')
            .respond({
                name: 'jhjhjh',
                artistId: 1,
                facebookUrl: 'klklklkl',
                websites : ['www.jkjkjk/facebook.com', 'www.jljl/twitter.fr', 'http://abc/soundcloud.com', 'www.hjhhjh.com'],
                tracks : ['fgfgfgfgfm', 'gfgfggfg', 'fgfgffgfgf', 'dfdfdfdff']
            });
        eventsHandler = $httpBackend.when('GET', '/artists/1/events')
            .respond(['event1', 'event2']);

        _$routeParams_.facebookUrl = 'klklklkl';
        _$rootScope_.artisteToCreate = false;

        $scope = _$rootScope_;
        $ctrl = $controller('ArtistCtrl', {
            $scope: $scope
        });
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it('should return $scope.artist with events', function () {
        $httpBackend.flush();
        var expectedArtist = {
            name: 'jhjhjh',
            artistId: 1,
            facebookUrl: 'klklklkl',
            websites : [
                {
                url : 'www.jkjkjk/facebook.com',
                name : 'facebook'
                },
                {
                    url : 'www.jljl/twitter.fr',
                    name : 'twitter'
                },
                {
                    url : 'http://abc/soundcloud.com',
                    name : 'soundcloud'
                },
                {
                    url : 'www.hjhhjh.com',
                    name : 'website'
                }
            ],
            tracks : ['fgfgfgfgfm', 'gfgfggfg', 'fgfgffgfgf', 'dfdfdfdff'],
            events : ['event1', 'event2']
        };

        expect($scope.artist).toEqual(expectedArtist);
    });

    it('should return $scope.website normalized', function () {
        $httpBackend.flush();
        var expectedWebsites = {
            iconWebsites : [
                {
                    url : 'www.jkjkjk/facebook.com',
                    name : 'facebook'
                },
                {
                    url : 'www.jljl/twitter.fr',
                    name : 'twitter'
                },
                {
                    url : 'http://abc/soundcloud.com',
                    name : 'soundcloud'
                }
            ],
            otherWebsites : [
                {
                    url : 'www.hjhhjh.com',
                    name : 'website'
                }
            ],
            facebookUrl : 'klklklkl'
        };

        expect($scope.websites).toEqual(expectedWebsites);
    })
});