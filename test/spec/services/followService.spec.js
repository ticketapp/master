'use strict';
describe('Factory: FollowService', function () {
    var FollowService,
        $httpBackend;

    beforeEach(module('claudeApp'));
    beforeEach(inject(function (_FollowService_, _$httpBackend_) {
        FollowService = _FollowService_;
        $httpBackend = _$httpBackend_;
        $httpBackend.when('GET', '/organizers/followed/')
            .respond([
                {id: 1, name: 'orga'},
                {id: 2, name: 'orga1'},
                {id: 3, name: 'orga2'}
            ]);
        $httpBackend.when('POST', '/organizers/4/followByOrganizerId')
            .respond('created');
        $httpBackend.when('POST', '/organizers/4/unfollowOrganizerByOrganizerId')
            .respond('created');
        $httpBackend.when('POST', '/organizers/123/followByFacebookId')
            .respond('created');
        $httpBackend.when('GET', '/organizers/3/isFollowed')
            .respond(true);
        $httpBackend.when('GET', '/places/followed/')
            .respond([
                {id: 1, name: 'place'},
                {id: 2, name: 'place1'},
                {id: 3, name: 'place2'}
            ]);
        $httpBackend.when('POST', '/places/4/followByPlaceId')
            .respond('created');
        $httpBackend.when('POST', '/places/4/unfollowPlaceByPlaceId')
            .respond('created');
        $httpBackend.when('POST', '/places/123/followByFacebookId')
            .respond('created');
        $httpBackend.when('GET', '/places/3/isFollowed')
            .respond(true);
        $httpBackend.when('GET', '/artists/followed/')
            .respond([
                {id: 1, name: 'artist'},
                {id: 2, name: 'artist1'},
                {id: 3, name: 'artist2'}
            ]);
        $httpBackend.when('POST', '/artists/4/followByArtistId')
            .respond('created');
        $httpBackend.when('POST', '/artists/4/unfollowArtistByArtistId')
            .respond('created');
        $httpBackend.when('POST', '/artists/123/followByFacebookId')
            .respond('created');
        $httpBackend.when('GET', '/artists/3/isFollowed')
            .respond(true);
        $httpBackend.when('GET', '/events/followed/')
            .respond([
                {id: 1, name: 'event'},
                {id: 2, name: 'event1'},
                {id: 3, name: 'event2'}
            ]);
        $httpBackend.when('POST', '/events/4/follow')
            .respond('created');
        $httpBackend.when('POST', '/events/4/unfollow')
            .respond('created');
        $httpBackend.when('GET', '/events/3/isFollowed')
            .respond(true);
        $httpBackend.when('GET', '/tracks/favorites')
            .respond([
                {trackId: 1, name: 'track'},
                {trackId: 2, name: 'track1'},
                {trackId: 3, name: 'track2'}
            ]);
        $httpBackend.when('POST', '/tracks/4/addToFavorites')
            .respond('created');
        $httpBackend.when('POST', '/tracks/4/removeFromFavorites')
            .respond('created');
        $httpBackend.when('GET', '/tracks/3/isFollowed')
            .respond(true);
    }));
    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it('should get followed organizers', function () {
        FollowService.organizers.followed();
        var expectedOrganizers = [
            {id: 1, name: 'orga'},
            {id: 2, name: 'orga1'},
            {id: 3, name: 'orga2'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedOrganizers).toEqual(expectedOrganizers);
    });

    it('should follow an organizer by id and add it to followedOrganizer storage', function() {
        FollowService.organizers.followByIdAndPushToFollowedOrganizers({id: 4, name: 'orga3'});
        var expectedOrganizers = [
            {id: 1, name: 'orga'},
            {id: 2, name: 'orga1'},
            {id: 3, name: 'orga2'},
            {id: 4, name: 'orga3'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedOrganizers).toEqual(expectedOrganizers);
    });

    it('should follow an organizer by facebookId and add it to followedOrganizer storage', function() {
        FollowService.organizers.followByFacebookIdAndPushToFollowedOrganizers({id: 4, name: 'orga3', facebookId: 123});
        var expectedOrganizers = [
            {id: 1, name: 'orga'},
            {id: 2, name: 'orga1'},
            {id: 3, name: 'orga2'},
            {id: 4, name: 'orga3', facebookId: 123}
        ];
        $httpBackend.flush();
        expect(FollowService.followedOrganizers).toEqual(expectedOrganizers);
    });

    it('should get organizer is followed', function() {
        var isFollowed = FollowService.organizers.isFollowed(3);
        $httpBackend.flush();
        expect(isFollowed.$$state.value).toEqual(true);
    });

    it('should unfollow an organizer by id and remove it to followedOrganizer storage', function() {
        FollowService.organizers.followByIdAndPushToFollowedOrganizers({id: 4, name: 'orga3'});
        var expectedOrganizers = [
            {id: 1, name: 'orga'},
            {id: 2, name: 'orga1'},
            {id: 3, name: 'orga2'},
            {id: 4, name: 'orga3'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedOrganizers).toEqual(expectedOrganizers);

        FollowService.organizers.unfollowById(4);

        var expectedOrganizers1 = [
            {id: 1, name: 'orga'},
            {id: 2, name: 'orga1'},
            {id: 3, name: 'orga2'}
        ];

        $httpBackend.flush();
        expect(FollowService.followedOrganizers).toEqual(expectedOrganizers1);
    });

    it('should get followed places', function () {
        FollowService.places.followed();
        var expectedPlaces = [
            {id: 1, name: 'place'},
            {id: 2, name: 'place1'},
            {id: 3, name: 'place2'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedPlaces).toEqual(expectedPlaces);
    });

    it('should follow an place by id and add it to followedPlace storage', function() {
        FollowService.places.followByIdAndPushToFollowedPlaces({id: 4, name: 'place3'});
        var expectedPlaces = [
            {id: 1, name: 'place'},
            {id: 2, name: 'place1'},
            {id: 3, name: 'place2'},
            {id: 4, name: 'place3'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedPlaces).toEqual(expectedPlaces);
    });

    it('should follow an place by facebookId and add it to followedPlace storage', function() {
        FollowService.places.followByFacebookIdAndPushToFollowedPlaces({id: 4, name: 'place3', facebookId: 123});
        var expectedPlaces = [
            {id: 1, name: 'place'},
            {id: 2, name: 'place1'},
            {id: 3, name: 'place2'},
            {id: 4, name: 'place3', facebookId: 123}
        ];
        $httpBackend.flush();
        expect(FollowService.followedPlaces).toEqual(expectedPlaces);
    });

    it('should get place is followed', function() {
        var isFollowed = FollowService.places.isFollowed(3);
        $httpBackend.flush();
        expect(isFollowed.$$state.value).toEqual(true);
    });

    it('should unfollow an place by id and remove it to followedPlace storage', function() {
        FollowService.places.followByIdAndPushToFollowedPlaces({id: 4, name: 'place3'});
        var expectedPlaces = [
            {id: 1, name: 'place'},
            {id: 2, name: 'place1'},
            {id: 3, name: 'place2'},
            {id: 4, name: 'place3'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedPlaces).toEqual(expectedPlaces);

        FollowService.places.unfollowById(4);

        var expectedPlaces1 = [
            {id: 1, name: 'place'},
            {id: 2, name: 'place1'},
            {id: 3, name: 'place2'}
        ];

        $httpBackend.flush();
        expect(FollowService.followedPlaces).toEqual(expectedPlaces1);
    });

    it('should get followed artists', function () {
        FollowService.artists.followed();
        var expectedArtists = [
            {id: 1, name: 'artist'},
            {id: 2, name: 'artist1'},
            {id: 3, name: 'artist2'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedArtists).toEqual(expectedArtists);
    });

    it('should follow an artist by id and add it to followedArtist storage', function() {
        FollowService.artists.followByIdAndPushToFollowedArtists({id: 4, name: 'artist3'});
        var expectedArtists = [
            {id: 1, name: 'artist'},
            {id: 2, name: 'artist1'},
            {id: 3, name: 'artist2'},
            {id: 4, name: 'artist3'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedArtists).toEqual(expectedArtists);
    });

    it('should follow an artist by facebookId and add it to followedArtist storage', function() {
        FollowService.artists.followByFacebookIdAndPushToFollowedArtists({id: 4, name: 'artist3', facebookId: 123});
        var expectedArtists = [
            {id: 1, name: 'artist'},
            {id: 2, name: 'artist1'},
            {id: 3, name: 'artist2'},
            {id: 4, name: 'artist3', facebookId: 123}
        ];
        $httpBackend.flush();
        expect(FollowService.followedArtists).toEqual(expectedArtists);
    });

    it('should get artist is followed', function() {
        var isFollowed = FollowService.artists.isFollowed(3);
        $httpBackend.flush();
        expect(isFollowed.$$state.value).toEqual(true);
    });

    it('should unfollow an artist by id and remove it to followedArtist storage', function() {
        FollowService.artists.followByIdAndPushToFollowedArtists({id: 4, name: 'artist3'});
        var expectedArtists = [
            {id: 1, name: 'artist'},
            {id: 2, name: 'artist1'},
            {id: 3, name: 'artist2'},
            {id: 4, name: 'artist3'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedArtists).toEqual(expectedArtists);

        FollowService.artists.unfollowById(4);

        var expectedArtists1 = [
            {id: 1, name: 'artist'},
            {id: 2, name: 'artist1'},
            {id: 3, name: 'artist2'}
        ];

        $httpBackend.flush();
        expect(FollowService.followedArtists).toEqual(expectedArtists1);
    });

    it('should get favorites tracks', function () {
        FollowService.tracks.favorites();
        var expectedTracks = [
            {trackId: 1, name: 'track'},
            {trackId: 2, name: 'track1'},
            {trackId: 3, name: 'track2'}
        ];
        $httpBackend.flush();
        expect(FollowService.favoritesTracks).toEqual(expectedTracks);
    });

    it('should add a track to favorite by id and add it to followedTrack storage', function() {
        FollowService.tracks.addToFavoriteAndAddToFactoryFavoritesTracks({trackId: 4, name: 'track3'});
        var expectedTracks = [
            {trackId: 1, name: 'track'},
            {trackId: 2, name: 'track1'},
            {trackId: 3, name: 'track2'},
            {trackId: 4, name: 'track3'}
        ];
        $httpBackend.flush();
        expect(FollowService.favoritesTracks).toEqual(expectedTracks);
    });

    it('should get track is followed', function() {
        var isFollowed = FollowService.tracks.isFollowed(3);
        $httpBackend.flush();
        expect(isFollowed.$$state.value).toEqual(true);
    });

    it('should remove a track frome favorites by trackid and remove it to followedTrack storage', function() {
        FollowService.tracks.addToFavoriteAndAddToFactoryFavoritesTracks({trackId: 4, name: 'track3'});
        var expectedTracks = [
            {trackId: 1, name: 'track'},
            {trackId: 2, name: 'track1'},
            {trackId: 3, name: 'track2'},
            {trackId: 4, name: 'track3'}
        ];
        $httpBackend.flush();
        expect(FollowService.favoritesTracks).toEqual(expectedTracks);

        FollowService.tracks.removeFromFavorites(4);

        var expectedTracks1 = [
            {trackId: 1, name: 'track'},
            {trackId: 2, name: 'track1'},
            {trackId: 3, name: 'track2'}
        ];

        $httpBackend.flush();
        expect(FollowService.favoritesTracks).toEqual(expectedTracks1);
    });

    it('should get followed events', function () {
        FollowService.events.followed();
        var expectedEvents = [
            {id: 1, name: 'event'},
            {id: 2, name: 'event1'},
            {id: 3, name: 'event2'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedEvents).toEqual(expectedEvents);
    });

    it('should follow an event by id and add it to followedEvent storage', function() {
        FollowService.events.followByIdAndPushToFollowedEvents({id: 4, name: 'event3'});
        var expectedEvents = [
            {id: 1, name: 'event'},
            {id: 2, name: 'event1'},
            {id: 3, name: 'event2'},
            {id: 4, name: 'event3'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedEvents).toEqual(expectedEvents);
    });

    it('should get event is followed', function() {
        var isFollowed = FollowService.events.isFollowed(3);
        $httpBackend.flush();
        expect(isFollowed.$$state.value).toEqual(true);
    });

    it('should unfollow an event by id and remove it to followedEvent storage', function() {
        FollowService.events.followByIdAndPushToFollowedEvents({id: 4, name: 'event3'});
        var expectedEvents = [
            {id: 1, name: 'event'},
            {id: 2, name: 'event1'},
            {id: 3, name: 'event2'},
            {id: 4, name: 'event3'}
        ];
        $httpBackend.flush();
        expect(FollowService.followedEvents).toEqual(expectedEvents);

        FollowService.events.unfollow(4);

        var expectedEvents1 = [
            {id: 1, name: 'event'},
            {id: 2, name: 'event1'},
            {id: 3, name: 'event2'}
        ];

        $httpBackend.flush();
        expect(FollowService.followedEvents).toEqual(expectedEvents1);
    });
});