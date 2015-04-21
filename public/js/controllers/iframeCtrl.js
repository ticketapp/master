app.controller('iframeCtrl', function ($http, $scope, $rootScope, $timeout, $filter, EventsFactory){
    var offset =  0;
    $scope.search = '';
    $scope.time = 30;
    $scope.limit = 12;
    $scope.events = [];
    var _selStart = $scope.limit;
    var textSlider;
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
                $scope.events.splice(e, 1);
                e = e - 1;
                eventsLenght = eventsLenght - 1;
            }
        }
    }
    function getEvents() {
        filterEventsByTime();
        console.log(_selStart)
        EventsFactory.getEvents(_selStart, $rootScope.geoLoc, offset).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getEventsByContaining() {
        EventsFactory.getEventsByContaining($scope.search, $rootScope.geoLoc).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getEventsArtistByContaining() {
        EventsFactory.getArtistsEventsByContaining($scope.search).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getEventsByGenre() {
        EventsFactory.getEventsByGenre($scope.search, offset).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getPlacesEventsByContaining() {
        EventsFactory.getPlacesEventsByContaining($scope.search).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    function getEventsByCity() {
        EventsFactory.getEventsByCity($scope.search, offset).then(function (events) {
            events.forEach(colorEvent);
            updateScope(events, $scope.events, 'eventId');
        });
    }

    $scope.updateEvents = function () {
        if ($scope.search.length == 0) {
            getEvents()
        } else {
            getEventsByContaining();
            getEventsArtistByContaining();
            getPlacesEventsByContaining();
            getEventsByCity();
            getEventsByGenre();
        }
    };
    $scope.research = function () {
        $scope.events = $filter('filter')($scope.events, {name: $scope.search});
        $scope.updateEvents();
    };
    $scope.moreLimit = function () {
        offset = offset + 12;
        $scope.limit = $scope.limit + 12;
        $scope.updateEvents();
    };
    $scope.initializeTime = function () {
        var newName = $scope.time;
        if (newName > 23 && newName <= 38) {
            newName = (newName - 23) * 24
        } else if (newName > 38 && newName <= 40) {
            newName = (newName - 36) * 168;
        } else if (newName > 40) {
            newName = (newName - 39) * 720;
        }
        _selStart = newName;
        getEvents();
        for (var i = 0; i < textSlider.length; i++) {
            textSlider[i].innerHTML = '';
            textSlider[i].innerHTML = textSlider[i].innerHTML + '<b style="color: #ffffff">' +
                $filter('millSecondsToTimeString')(newName) + '</b>';
        }
    };
    var waitForSlider = setInterval(function () {
        if ($rootScope.window == 'small' || $rootScope.window == 'medium') {
            textSlider = document.getElementById('timeSearchSliderPhone').getElementsByClassName('md-thumb');
        } else {
            textSlider = document.getElementById('timeSearchSlider').getElementsByClassName('md-thumb');
        }
        if (textSlider.length > 0) {
            clearInterval(waitForSlider)
            $scope.initializeTime();
        }
    }, 100);
    $rootScope.$watch('geoLoc', function (newval) {
        if (newval.length > 0) {
            getEvents();
        }
    })
});