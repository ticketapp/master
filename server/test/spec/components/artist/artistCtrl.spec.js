'use strict';

describe('Controller: ArtistCtrl', function () {
    var $ctrl,
        $scope,
        $timeout,
        $httpBackend,
        ArtistsFactory,
        isFollowed;
    // load the controller's module
    beforeEach(module('claudeApp'));

    beforeEach(inject(function ($controller, _$rootScope_, _$routeParams_, _ArtistsFactory_, _$httpBackend_, _$timeout_) {
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
        ArtistsFactory = _ArtistsFactory_;

        $httpBackend.when('GET', '/artists/klklklkl')
            .respond({
                name: 'jhjhjh',
                artistId: 1,
                facebookUrl: 'klklklkl',
                websites : ['www.jkjkjk/facebook.com', 'www.jljl/twitter.fr', 'http://abc/soundcloud.com', 'www.hjhhjh.com'],
                tracks : [{name: 'fgfgfgfgfm', url: 'aa'}]
            });
        $httpBackend.when('GET', '/artists/klklklkl/events')
            .respond([]);
        $httpBackend.when('GET', '/artists/1/isFollowed')
            .respond(true);
        $httpBackend.when('GET', '/tracks/jhjhjh/klklklkl/trackTitle')
            .respond([{name: 'fgfgfgfgfm', url: 'aa'}, {name: 'aaaaa', url: 'aaaa'}]);

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
        $timeout.flush();
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
            tracks : [{name: 'fgfgfgfgfm', url: 'aa'}]
        };

        expect($scope.artist).toEqual(expectedArtist);
    });

    it('should return $scope.website normalized', function () {
        $httpBackend.flush();
        $timeout.flush();
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
    });

    it('should return suggested tracks without duplicate', function () {
        $httpBackend.flush();
        $timeout.flush();
        $scope.suggestQuery('trackTitle', 'jhjhjh', 'klklklkl');
        $httpBackend.flush();
        var expectedTracks = [
            {name: 'fgfgfgfgfm', url: 'aa'},
            {name: 'aaaaa', url: 'aaaa'}
        ];
        expect($scope.artist.tracks).toEqual(expectedTracks);
    });

    it('should refactor artist name', function () {
        $httpBackend.flush();
        $timeout.flush();
        var names = [
            "lkjlkj officiel",
            "lkjlkj official",
            "lkjlkj fanpage",
            "lkjlkj music",
            "lkjlkj musik",
            "lkjlkj musique",
            "lkjlkj / lkjlkj",
            "lkjlkj * lkjlkj",
            "lkjlkj - lkjlkj",
            "lkjlkj*lkjlkj",
            "l'kjlkj",
            "lékjlkj"
        ];
        var expectedRefactoredNames = [
            "lkjlkj",
            "lkjlkj",
            "lkjlkj",
            "lkjlkj",
            "lkjlkj",
            "lkjlkj",
            "lkjlkj lkjlkj",
            "lkjlkj lkjlkj",
            "lkjlkj lkjlkj",
            "lkjlkj lkjlkj",
            "l'kjlkj",
            "lékjlkj"
        ];

        names = names.map(function (name) {
            return ArtistsFactory.refactorArtistName(name);
        });

        expect(names).toEqual(expectedRefactoredNames);
    });
});