angular.module('claudeApp').
    controller('IframeCtrl', ['$scope', '$rootScope', '$timeout', '$filter', 'EventsFactory',
    function ($scope, $rootScope, $timeout, $filter, EventsFactory){
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
            var idDictionnary = {'id': el.id, 'facebookId': el.facebookId};
            scopeIdList.push(idDictionnary[idName]);
        }
        if (otherScopeToCheck != undefined) {
            otherScopeToCheck.forEach(getId);
        }
        scope.forEach(getId);
        function pushEl (el, index, array) {
            var idDictionnary = {'id': el.id, 'facebookId': el.facebookId};
            if (scopeIdList.indexOf(idDictionnary[idName]) == -1) {
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
        EventsFactory.getEvents(_selStart, $rootScope.geoLoc, offset).then(function (events) {
            updateScope(events, $scope.events, 'id');
        });
    }

    function getEventsByContaining() {
        EventsFactory.getEventsByContaining($scope.search, $rootScope.geoLoc).then(function (events) {
            updateScope(events, $scope.events, 'id');
        });
    }

    function getEventsArtistByContaining() {
        EventsFactory.getArtistsEventsByContaining($scope.search).then(function (events) {
            updateScope(events, $scope.events, 'id');
        });
    }

    function getEventsByGenre() {
        EventsFactory.getEventsByGenre($scope.search, offset).then(function (events) {
            updateScope(events, $scope.events, 'id');
        });
    }

    function getPlacesEventsByContaining() {
        EventsFactory.getPlacesEventsByContaining($scope.search).then(function (events) {
            updateScope(events, $scope.events, 'id');
        });
    }

    function getEventsByCity() {
        EventsFactory.getEventsByCity($scope.search, offset).then(function (events) {
            updateScope(events, $scope.events, 'id');
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
}]);