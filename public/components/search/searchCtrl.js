angular.module('claudeApp').controller('searchCtrl', ['$scope', '$rootScope', '$filter', '$timeout',
    'ArtistsFactory', 'EventsFactory', 'OrganizerFactory', 'PlaceFactory', 'SearchFactory', 'RefactorGeopoint',
    function($rootScope, $scope, $filter, $timeout, ArtistsFactory, EventsFactory, OrganizerFactory,
             PlaceFactory, SearchFactory, RefactorGeopoint){
        SearchFactory.init();
        $scope.limit = 12;
        $scope.artists = [];
        $scope.artistsFb = [];
        $scope.organizers = [];
        $scope.places = [];
        $scope.events = [];
        $scope.filtredEvents = [];
        $scope.loadingMore = true;
        var offset = 0;
        var _selArtist = $rootScope.activArtist;
        var _selEvent = $rootScope.activEvent;
        var _selOrganizer = $rootScope.activUsr;
        var _selPlace = $rootScope.activPlace;
        var _selStart;
        if ($rootScope.storeSearch != undefined && $rootScope.storeSearch.length > 0) {
            var _research = $rootScope.storeSearch;
            $rootScope.remStoreSearch();
        } else if (document.getElementById('searchBar') != null) {
            var _research = document.getElementById('searchBar').value.trim();
        } else {
            var _research = '';
        }

        function refactorGeopoint (place) {
            if (place.geographicPoint) {
                place.geographicPoint = RefactorGeopoint.refactorGeopoint(place.geographicPoint)
            }
        }
        function updateScope (data, scope, idName, otherScopeToCheck) {
            var scopeIdList = [];
            function getId(el, index, array) {
                var idDictionary = {'artistId': el.artistId, 'eventId': el.eventId, 'organizerId': el.organizerId,
                    'placeId': el.placeId, 'facebookId': el.facebookId};
                scopeIdList.push(idDictionary[idName]);
            }
            if (otherScopeToCheck != undefined) {
                otherScopeToCheck.forEach(getId);
            }
            scope.forEach(getId);
            function pushEl (el, index, array) {
                var idDictionary = {'artistId': el.artistId, 'eventId': el.eventId,
                    'organizerId': el.organizerId, 'placeId': el.placeId, 'facebookId': el.facebookId};
                if (scopeIdList.indexOf(idDictionary[idName]) == -1) {
                    scopeIdList.push(idDictionary[idName]);
                    $timeout(function () {
                        $scope.$apply(function () {
                            scope.push(el);
                        })
                    }, 0);
                }
            }
            data.forEach(pushEl);
            $scope.loadingMore = false;
        }

        function filterEventsByTime() {
            var eventsLength = $scope.events.length;
            var maxStartTime = _selStart * 3600000 + new Date().getTime();
            for (var e = 0; e < eventsLength; e++) {
                if ($scope.events[e].startTime > maxStartTime) {
                    $scope.events.splice(e, 1)
                    e = e - 1;
                    eventsLength = eventsLength - 1;
                }
            }
        }
        function getArtists () {
            ArtistsFactory.getArtists(offset).then(function (artists) {
                updateScope(artists, $scope.artists, 'artistId');
            });
        }

        function getArtistsFolowed () {
            ArtistsFactory.getFollowArtists().then(function (artists) {
                updateScope(artists, $scope.artists, 'artistId');
            })
        }

        function getArtistsByGenre () {
            ArtistsFactory.getArtistsByGenre(offset, _research).then(function (artists) {
                updateScope(artists, $scope.artists, 'artistId');
            })
        }
        function getArtistsByContaining () {
            ArtistsFactory.getArtistsByContaining(_research).then(function (artists) {
                updateScope(artists, $scope.artists, 'artistId');
            });
        }

        function getEvents() {
            filterEventsByTime();
            EventsFactory.getEvents(_selStart, $rootScope.geoLoc, offset).then(function (events) {
                updateScope(events, $scope.filtredEvents, 'eventId', $scope.events);
            });
        }

        function getEventsArtistByContaining() {
            EventsFactory.getArtistsEventsByContaining(_research).then(function (events) {
                updateScope(events, $scope.events, 'eventId');
            });
        }

        function getEventsByGenre() {
            EventsFactory.getEventsByGenre(_research, offset, $rootScope.geoLoc).then(function (events) {
                updateScope(events, $scope.events, 'eventId');
                console.log(events)
            });
        }

        function getPlacesEventsByContaining() {
            EventsFactory.getPlacesEventsByContaining(_research).then(function (events) {
                updateScope(events, $scope.events, 'eventId');
            });
        }

        function getEventsByCity() {
            EventsFactory.getEventsByCity(_research, offset).then(function (events) {
                updateScope(events, $scope.events, 'eventId');
            });
        }

        function getEventsByContaining() {
            EventsFactory.getEventsByContaining(_research, $rootScope.geoLoc).then(function (events) {
                updateScope(events, $scope.filtredEvents, 'eventId', $scope.events);
                console.log(events);
                getEventsArtistByContaining();
                getEventsByGenre();
                getPlacesEventsByContaining();
                getEventsByCity();
            });
        }

        function getOrganizersByContaining() {
            OrganizerFactory.getOrganizersByContaining(_research).then(function (organizers) {
                updateScope(organizers, $scope.organizers, 'organizerId');
            });
        }

        function getOrganizers() {
            OrganizerFactory.getOrganizers(offset).then(function (organizers) {
                updateScope(organizers, $scope.organizers, 'organizerId');
            });
        }

        function getPlaces() {
            PlaceFactory.getPlaces(offset, $rootScope.geoLoc).then(function (places) {
                places.forEach(refactorGeopoint);
                updateScope(places, $scope.places, 'placeId');
            });
        }

        function getPlacesByContaining() {
            PlaceFactory.getPlacesByContaining(_research).then(function (places) {
                updateScope(places, $scope.places, 'placeId');
            });
        }

        function getPlacesByCity() {
            PlaceFactory.getPlacesByCity(_research, offset).then(function (places) {
                updateScope(places, $scope.places, 'placeId');
            });
        }

        function getArtistsFacebook() {
            $scope.loadingFbArt = true;
            ArtistsFactory.getArtistsFacebookByContaining(_research).then(function (artists) {
                updateScope(artists, $scope.artistsFb, 'facebookId', $scope.artists);
                $scope.loadingFbArt = false;
            });
        }
        function search () {
            if (_selArtist == true) {
                if (_research.length == 0) {
                    if (offset == 0 && $rootScope.connected == true) {
                        getArtistsFolowed();
                        getArtists()
                    } else {
                        getArtists()
                    }
                } else {
                    getArtistsByContaining();
                    getArtistsByGenre()
                }
            }
            if (_selPlace == true) {
                if (_research.length == 0) {
                    getPlaces()
                } else {
                    getPlacesByContaining();
                    getPlacesByCity();
                }
            }
            if (_selOrganizer == true) {
                if (_research.length == 0) {
                    getOrganizers()
                } else {
                    getOrganizersByContaining();
                }
            }
            if (_selEvent == true) {
                if (_research.length == 0) {
                    getEvents()
                } else {
                    getEventsByContaining();
                }
            }
        }

        $scope.initializeTime = function () {
            var newName = $rootScope.maxStart;
            if (newName > 23 && newName <= 38) {
                newName = (newName - 23) * 24
            } else if (newName > 38 && newName <= 40) {
                newName = (newName - 36) * 168;
            } else if (newName > 40) {
                newName = (newName - 39) * 720;
            }
            _selStart = newName;
            var waitForSearchBar = setInterval(function () {
                if ($rootScope.window == 'small' || $rootScope.window == 'medium') {
                    var textSlider = document.getElementById('timeSearchSliderPhone').getElementsByClassName('md-thumb');
                } else {
                    var slider = document.getElementsByClassName('bigSlider');
                    var textSlider = [];
                    for (var ii =0; ii < slider.length; ii++) {
                        textSlider.push(slider[ii].getElementsByClassName('md-thumb')[0]);
                    }
                }
                if ($rootScope.path == 'search' || textSlider.length >= 2) {
                    clearInterval(waitForSearchBar);
                    for (var i = 0; i < textSlider.length; i++) {
                        textSlider[i].innerHTML = '';
                        textSlider[i].innerHTML = textSlider[i].innerHTML + '<b style="color: #ffffff">' +
                            $filter('millSecondsToTimeString')(newName) + '</b>';
                    }
                } else if (_selEvent == false || _research.length > 0) {
                    clearInterval(waitForSearchBar);
                }
            }, 100);
        };
        $scope.initializeTime();
        $scope.moreLimit = function () {
            offset = offset + 12;
            $scope.limit = $scope.limit + 12;
            $scope.loadingMore = true;
            search();
        };
        var facebookSearch;
        var facebookSearchInterval = 600;
        var otherSearch;
        var otherSearchInterval = 600;
        $scope.research = function(newName) {
            if (angular.isDefined(newName) && (_selArtist == true || _selEvent == true || _selOrganizer == true ||
                _selPlace == true)) {
                $scope.loadingMore = true;
                _research = newName;
                $scope.searchPat = newName;
                $scope.limit = 12;
                offset = 0;
                if (_research.length == 0) {
                    $scope.initializeTime()
                } else {
                    $scope.events = $filter('filter')($scope.events, {name: _research});
                    $scope.filtredEvents = $filter('filter')($scope.filtredEvents, {name: _research});
                    $scope.organizers = $filter('filter')($scope.organizers, {nickname :  _research});
                    $scope.places = $filter('filter')($scope.places, {name :  _research});
                    $scope.artistsFb = $filter('filter')($scope.artistsFb, {name :  _research});
                    $scope.artists = $filter('filter')($scope.artists, {name: _research});
                }
                if (_selArtist == true && _research.length > 2 &&
                    newName != 'electro' &&
                    newName != 'reggae' &&
                    newName != 'rock' &&
                    newName != 'jazz' &&
                    newName != 'musique du monde' &&
                    newName != 'musique latine' &&
                    newName != 'classique' &&
                    newName != 'hip-hop' &&
                    newName != 'chanson'
                    ) {
                    clearTimeout(facebookSearch);
                    facebookSearch = setTimeout(getArtistsFacebook, facebookSearchInterval);
                }
                clearTimeout(otherSearch);
                otherSearch = setTimeout(search, otherSearchInterval);
            }
            SearchFactory.storeSearch(_research);
            return _research;
        };
        $scope.selArtist = function(newName) {
            if (angular.isDefined(newName)) {
                _selArtist = newName;
                $scope.limit = 12;
                offset = 0;
                if (newName == true) {
                    $scope.loadingMore = true;
                    if (_research.length == 0) {
                        if ($rootScope.connected == true && offset == 0) {
                            getArtistsFolowed();
                        }
                        getArtists()
                    } else {
                        getArtistsByContaining();
                        getArtistsByGenre();
                        if ( _research.length > 2 &&
                            _research != 'electro' &&
                            _research != 'reggae' &&
                            _research != 'rock' &&
                            _research != 'jazz' &&
                            _research != 'musique du monde' &&
                            _research != 'musique latine' &&
                            _research != 'classique' &&
                            _research != 'hip-hop' &&
                            _research != 'chanson'
                            ) {
                            getArtistsFacebook();
                        }
                    }
                } else {
                    $scope.artists = []
                }
            }
            return _selArtist;
        };
        $scope.selEvent = function(newName) {
            if (angular.isDefined(newName)) {
                _selEvent = newName;
                $scope.limit = 12;
                offset = 0;
                if (newName == true) {
                    $scope.loadingMore = true;
                    if (_research.length == 0) {
                        $scope.initializeTime()
                        getEvents()
                    } else {
                        getEventsByContaining();
                        getEventsArtistByContaining();
                        getEventsByCity();
                        getEventsByGenre();
                        getPlacesEventsByContaining()
                    }
                } else {
                    $scope.events = []
                }
            }
            return _selEvent;
        };
        $scope.selPlace = function(newName) {
            if (angular.isDefined(newName)) {
                _selPlace = newName;
                $scope.limit = 12;
                offset = 0;
                if (newName == true) {
                    $scope.loadingMore = true;
                    if (_research.length == 0) {
                        getPlaces()
                    } else {
                        getPlacesByContaining();
                        getPlacesByCity()
                    }
                } else {
                    $scope.places = []
                }
            }
            return _selPlace;
        };
        $scope.selUsr = function(newName) {
            if (angular.isDefined(newName)) {
                _selOrganizer = newName;
                $scope.limit = 12;
                offset = 0;
                if (newName == true) {
                    $scope.loadingMore = true;
                    if (_research.length == 0) {
                        getOrganizers()
                    } else {
                        getOrganizersByContaining()
                    }
                } else {
                    $scope.organizers = []
                }
            }
            return _selOrganizer;
        };
        var StartTimer;
        var doneStartInterval = 600;
        $scope.selStart = function(newName) {
            if (angular.isDefined(newName)) {
                $scope.limit = 12;
                offset = 0;
                $scope.initializeTime();
                if (newName > 23 && newName <= 38) {
                    newName = (newName - 23) * 24
                } else if (newName > 38 && newName <= 40) {
                    newName = (newName - 36) * 168;
                } else if (newName > 40) {
                    newName = (newName - 39) * 720;
                }
                _selStart = newName;
                clearTimeout(StartTimer);
                StartTimer = setTimeout(function () {
                    $scope.loadingMore = true;
                    filterEventsByTime();
                    getEvents()
                }, doneStartInterval);
                return _selStart;
            }
        };
        if ($rootScope.geoLoc.length > 0) {
            console.log($rootScope.geoLoc);
            search()
        } else {
            $rootScope.$watch('geoLoc', function (newVal) {
                console.log($rootScope.geoLoc);
                if (newVal.length > 0) {
                    $scope.events = [];
                    $scope.places = [];
                    $scope.organizers = [];
                    search()
                }
            })
        }
    }]);