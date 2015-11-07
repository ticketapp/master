'use strict';
describe('Factory: TrackService', function () {
    var TrackService,
        $localStorage;

    beforeEach(module('claudeApp'));
    beforeEach(inject(function (_TrackService_, _$localStorage_) {
        TrackService = _TrackService_;
        $localStorage = _$localStorage_;
        $localStorage.tracksSignaled = [{'trackId': 1}, {'trackId': 3}]
    }));

    it('should filter removed tracks', function () {
        var tracks = [
            {'uuid': 1},
            {'uuid': 2},
            {'uuid': 3},
            {'uuid': 4}
        ];
        var filteredTracks = TrackService.filterSignaledTracks(tracks);
        var expectedTracks = [
            {'uuid': 2},
            {'uuid': 4}
        ];
        expect(filteredTracks).toEqual(expectedTracks);
    });

    it('should count number of rates of an array of tracks', function () {
        var tracks = [
            {'uuid': 1, confidence: 6000},
            {'uuid': 2, confidence: 6000},
            {'uuid': 3, confidence: 4000},
            {'uuid': 4, confidence: 3000}
        ];
        var numberOfRate = TrackService.countRates(tracks);
        var expectednumber = 2;
        expect(numberOfRate).toEqual(expectednumber);
    });
});