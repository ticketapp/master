angular.module('claudeApp').controller('searchCtrl', ['$scope', '$rootScope', '$filter', '$timeout',
    'ArtistsFactory', 'EventsFactory', 'OrganizerFactory', 'PlaceFactory', 'SearchFactory', 'FollowService',
    function($rootScope, $scope, $filter, $timeout, ArtistsFactory, EventsFactory, OrganizerFactory,
             PlaceFactory, SearchFactory, FollowService){
        SearchFactory.init();
        $scope.limit = 12;
        $scope.artists = [];
        $scope.artistsFb = [];
        $scope.organizers = [];
        $scope.places = [];
        $scope.events = [];
        $scope.loadingMore = true;
        var offset = 0;
        var _selArtist = $rootScope.activArtist;
        var _selEvent = $rootScope.activEvent;
        var _selOrganizer = $rootScope.activUsr;
        var _selPlace = $rootScope.activPlace;
        var _selStart;

        var _research = '';
        if ($rootScope.storeSearch != undefined && $rootScope.storeSearch.length > 0) {
            _research = $rootScope.storeSearch;
            $rootScope.remStoreSearch();
        } else if (document.getElementById('searchBar') != null) {
            _research = document.getElementById('searchBar').value.trim();
        }

        function updateScope (data, scope, idName, otherScopeToCheck) {
            function isInScope(el) {
                var idDictionary = {'id': el.id, 'facebookId': el.facebookId};
                if (otherScopeToCheck !== undefined) {
                    var otherScopeToCheckLength = otherScopeToCheck.length;
                    for (var i = 0; i < otherScopeToCheckLength; i++) {
                        var idOtherScopeDictionary = {'artistId': otherScopeToCheck[i].artistId,
                            'id': otherScopeToCheck[i].id,
                            'facebookId': otherScopeToCheck[i].facebookId};
                        if (idDictionary[idName] == idOtherScopeDictionary[idName]) {
                            return true;
                        }
                    }
                }
                var scopeLength = scope.length;
                for (var j = 0; j < scopeLength; j++) {
                    var idScopeDictionary = {'id': scope[j].id,
                        'facebookId': scope[j].facebookId};
                    if (idDictionary[idName] == idScopeDictionary[idName]) {
                        return true;
                    }
                }
                return false;
            }

            function pushEl (el) {
                if (isInScope(el) == false) {
                    scope.push(el);
                }
            }
            $scope.loadingMore = false;
            data.forEach(pushEl);
        }

        function getArtists () {
            ArtistsFactory.getArtists(offset).then(function (artists) {
                updateScope(artists, $scope.artists, 'id');
            });
        }

        function getArtistsFollowed () {
            FollowService.artists.followed().then(function (artists) {
                updateScope(artists, $scope.artists, 'id');
                if (artists.length < $scope.limit) {
                    getArtists()
                }
            })
        }

        function getArtistsByGenre () {
            ArtistsFactory.getArtistsByGenre(offset, _research).then(function (artists) {
                updateScope(artists, $scope.artists, 'id');
            })
        }
        function getArtistsByContaining () {
            ArtistsFactory.getArtistsByContaining(_research).then(function (artists) {
                var artistsFacebookInScope = $scope.artistsFb.map(function (artist) {
                    return artist.id
                });
                var artistLength = artists.length;
                for (var i = 0; i < artistLength; i++) {
                    if (artistsFacebookInScope.indexOf(artists[i].id) > -1) {
                        $scope.artistsFb.splice(artistsFacebookInScope.indexOf(artists[i].id), 1)
                    }
                }
                updateScope(artists, $scope.artists, 'id');
            });
        }

        function getOrganizersByContaining() {
            OrganizerFactory.getOrganizersByContaining(_research).then(function (organizers) {
                updateScope(organizers, $scope.organizers, 'id');
            });
        }

        function getOrganizers() {
            OrganizerFactory.getOrganizers(offset).then(function (organizers) {
                updateScope(organizers, $scope.organizers, 'id');
            });
        }

        function getPlaces() {
            PlaceFactory.getPlaces(offset, $rootScope.geoLoc).then(function (places) {
                updateScope(places, $scope.places, 'id');
            });
        }

        function getPlacesByContaining() {
            PlaceFactory.getPlacesByContaining(_research).then(function (places) {
                updateScope(places, $scope.places, 'id');
            });
        }

        function getPlacesByCity() {
            PlaceFactory.getPlacesByCity(_research, offset).then(function (places) {
                updateScope(places, $scope.places, 'id');
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
                        getArtistsFollowed();
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
                    SearchFactory.getEvents(_selStart, offset).then(function(events) {
                        $scope.events = events;
                        $scope.loadingMore = false;
                    }, function(error) {

                    }, function(update) {
                        $scope.events = update;
                        $scope.loadingMore = false;
                    })
                } else {
                    SearchFactory.searchEventsWithQuery(_research, offset).then(function(events) {
                        $scope.events = events;
                        $scope.loadingMore = false;
                    }, function(error) {

                    }, function(update) {
                        $scope.events = update;
                        $scope.loadingMore = false;
                    })
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
                var textSlider = [];
                if ($rootScope.window == 'small' || $rootScope.window == 'medium') {
                    textSlider = document.getElementById('timeSearchSliderPhone').getElementsByClassName('md-thumb');
                } else {
                    var slider = document.getElementsByClassName('bigSlider');
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
            if (angular.isDefined(newName)) {
                _research = newName;
                if (_selArtist == true || _selEvent == true || _selOrganizer == true ||
                    _selPlace == true) {
                    $scope.loadingMore = true;
                    $scope.searchPat = newName;
                    $scope.limit = 12;
                    offset = 0;
                    if (_research.length == 0) {
                        $scope.initializeTime()
                    } else {
                        $scope.organizers = $filter('filter')($scope.organizers, {nickname: _research});
                        $scope.places = $filter('filter')($scope.places, {name: _research});
                        $scope.artistsFb = $filter('filter')($scope.artistsFb, {name: _research});
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
                    _selPlace = false;
                    _selEvent = false;
                    _selOrganizer = false;
                    $scope.loadingMore = true;
                    if (_research.length == 0) {
                        if ($rootScope.connected == true && offset == 0) {
                            getArtistsFollowed();
                        } else {
                            getArtists()
                        }
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
                    _selArtist = false;
                    _selPlace = false;
                    _selOrganizer = false;
                    if (_research.length == 0) {
                        $scope.initializeTime();
                        SearchFactory.getEvents(_selStart, offset).then(function(events) {
                            $scope.events = events;
                            $scope.loadingMore = false;
                        }, function(error) {

                        }, function(update) {
                            $scope.events = update;
                            $scope.loadingMore = false
                        })
                    } else {
                        SearchFactory.searchEventsWithQuery(_research, offset).then(function(events) {
                            $scope.events = events;
                            $scope.loadingMore = false;
                        }, function(error) {

                        }, function(update) {
                            $scope.events = update;
                            $scope.loadingMore = false
                        })
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
                    _selArtist = false;
                    _selEvent = false;
                    _selOrganizer = false;
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
                    _selArtist = false;
                    _selEvent = false;
                    _selPlace = false;
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
                    SearchFactory.getEvents(_selStart, offset).then(function(events) {
                        $scope.events = events;
                        $scope.loadingMore = false;
                    }, function(error) {

                    }, function(update) {
                        $scope.events = update;
                        $scope.loadingMore = false;
                    })
                }, doneStartInterval);
                return _selStart;
            }
        };
        if ($rootScope.geoLoc.length > 0) {
            search()
        } else {
            $rootScope.$watch('geoLoc', function (newVal) {
                if (newVal.length > 0) {
                    $scope.events = [];
                    $scope.places = [];
                    $scope.organizers = [];
                    search()
                }
            })
        }
        if (_selArtist == true && _research.length > 2 &&
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
    }]);