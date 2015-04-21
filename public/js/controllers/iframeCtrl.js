app.controller('iframeCtrl', function ($http, $scope, $rootScope, $timeout, $filter){
    var offset =  0;
    $scope.search = '';
    $scope.events = [];
    function updateScope (data, scope, idName, otherScopeToCheck) {
        var scopeIdList = [];
        function getId(el, index, array) {
            var idDictionnary = {'artistId': el.artistId, 'eventId': el.eventId, 'organizerId': el.organizerId,
                'placeId': el.placeId, 'facebookId': el.facebookId};
            scopeIdList.push(idDictionnary[idName]);
        }
        if (otherScopeToCheck != undefined) {
            otherScopeToCheck.forEach(getId);
        }
        scope.forEach(getId);
        function pushEl (el, index, array) {
            var idDictionnary = {'artistId': el.artistId, 'eventId': el.eventId, 'organizerId': el.organizerId,
                'placeId': el.placeId, 'facebookId': el.facebookId};
            if (scopeIdList.indexOf(idDictionnary[idName]) == -1) {
                $timeout(function () {
                    $scope.$apply(function () {
                        scope.push(el);
                    })
                }, 0);
            }
        }
        data.forEach(pushEl);
        $rootScope.resizeImgHeight();
        $scope.loadingMore = false;
    }
    function colorEvent(el) {
        el.priceColor = 'rgb(0, 140, 186)';
        if (el.tariffRange != undefined) {
            var tariffs = el.tariffRange.split('-');
            if (tariffs[1] > tariffs[0]) {
                el.tariffRange = tariffs[0].replace('.0', '') + ' - ' +
                    tariffs[1].replace('.0', '') + '€';
            } else {
                el.tariffRange = tariffs[0].replace('.0', '') + '€';
            }
            el.priceColor = 'rgb(' + tariffs[0] * 2 + ',' + (200 - (tariffs[0] * 4 ) ) +
                ',' + tariffs[0] * 4 + ')'
        }
    }

    function filterEventsByTime() {
        var eventsLenght = $scope.events.length;
        var maxStartTime = _selStart * 3600000 + new Date().getTime();
        for (var e = 0; e < eventsLenght; e++) {
            if ($scope.events[e].startTime > maxStartTime) {
                $scope.events.splice(e, 1)
                e = e - 1;
                eventsLenght = eventsLenght - 1;
            }
        }
    }
    function getEvents() {
        filterEventsByTime();
        EventsFactory.getEvents(_selStart, $rootScope.geoLoc, offset).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getEventsByContaining() {
        EventsFactory.getEventsByContaining(_research, $rootScope.geoLoc).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getEventsArtistByContaining() {
        EventsFactory.getArtistsEventsByContaining(_research).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getEventsByGenre() {
        EventsFactory.getEventsByGenre(_research, offset).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getPlacesEventsByContaining() {
        EventsFactory.getPlacesEventsByContaining(_research).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getEventsByCity() {
        EventsFactory.getEventsByCity(_research, offset).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    $scope.moreLimit = function () {
        offset = offset + 20;
        $rootScope.resizeImgHeight();
        getEvents();
    };
    $scope.initializeTime = function () {
        var newName = $rootScope.maxStart;
        if (newName > 23 && newName <= 38) {
            newName = (newName - 23) * 24
        } else if (newName > 38 && newName <= 40) {
            newName = (newName - 36) * 168;
        } else if (newName > 40) {
            newName = (newName - 39) * 720;
        }
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
    getEvents();
    $rootScope.$watch('geoLoc', function (newval) {
        if (newval.length > 0) {
            getEvents();
        }
    })
});