app.controller ('ArtistesCtrl', function ($scope, ArtisteFactory, $routeParams, $http, $rootScope, $websocket){
    $scope.bigTracks = true;
    $scope.trackLimit = 10;
    $scope.heightDesc = '147px';
    $scope.trackTitle = '';
    function refactorWebsites () {
        for (var i = 0; i < $scope.artiste.websites.length; i++) {
            $scope.artiste.websites[i] = {url: $scope.artiste.websites[i]};
            if ($scope.artiste.websites[i].url.indexOf('facebook') > -1) {
                $scope.artiste.websites[i].name = 'facebook';
            } else if ($scope.artiste.websites[i].url.indexOf('twitter') > -1) {
                $scope.artiste.websites[i].name = 'twitter';
            } else if ($scope.artiste.websites[i].url.indexOf('soundcloud') > -1) {
                $scope.artiste.websites[i].name = 'soundcloud';
            } else if ($scope.artiste.websites[i].url.indexOf('mixcloud') > -1) {
                $scope.artiste.websites[i].name = 'mixcloud';
            } else {
                $scope.artiste.websites[i].name = 'website';
                $scope.otherWebsite = true;
            }
        }
    }
    if ($rootScope.artisteToCreate == false) {
        console.log('not creat')
        $http.get('/artists/' + $routeParams.facebookUrl)
            .success(function (data, status) {
                $scope.artiste = data;
                refactorWebsites();
            }).error(function (data, status) {
            });
    } else {
        refactorWebsites();
        $rootScope.passArtisteToCreateToFalse();
        console.log($scope.artiste)
    }
    /*$http.get('/artists/' + $routeParams.id +'/events')
     .success(function(data, status){
     $scope.orgaEvents = data;*/
    $rootScope.resizeImgHeight();
    //console.log($scope.orgaEvents);
    if (/*$scope.orgaEvents.length > 0 &&*/ $rootScope.window != 'small' && $rootScope.window != 'medium') {
        var waitForBinding = setInterval(function () {
            if (document.getElementById('events_contener').innerHTML.length > 0) {
                clearInterval(waitForBinding);
                var eventInfoConteners = document.getElementsByClassName('eventInfo');
                /*if ($scope.orgaEvents.length == 1) {
                 document.getElementsByClassName('descriptionContent')[0].classList.remove('large-8');
                 document.getElementsByClassName('descriptionContent')[0].classList.add('large-4');
                 document.getElementsByClassName('descriptionContent')[0].classList.add('paddingLeft0');
                 document.getElementsByClassName('data-ng-event')[0].classList.add('width100p');
                 document.getElementsByClassName('min_contener')[0].classList.add('padding0');
                 $rootScope.resizeImgHeight();
                 var descPlace = document.getElementsByClassName('descriptionContent')[0].getBoundingClientRect();
                 for (var i = 0; i < eventInfoConteners.length; i++) {
                 eventInfoConteners[i].classList.remove('large-4');
                 eventInfoConteners[i].classList.add('large-8');
                 var infoPlace = eventInfoConteners[i].getBoundingClientRect();
                 if (infoPlace.top > descPlace.bottom) {
                 eventInfoConteners[i].classList.remove('large-8');
                 eventInfoConteners[i].classList.add('large-12');
                 }
                 }
                 }*/
                angular.element(document).ready(function () {
                    for (var i = 0; i < eventInfoConteners.length; i++) {
                        if (eventInfoConteners[i].offsetLeft < 30) {
                            eventInfoConteners[i].classList.remove('large-4');
                            eventInfoConteners[i].classList.remove('large-8');
                            eventInfoConteners[i].classList.add('large-12');
                            if (eventInfoConteners[i].className.indexOf('tracksContener') > -1) {
                                $scope.bigTracks = false;
                            }
                        }
                    }
                })
            }
        }, 100);
    } else {
        $rootScope.$watch('window', function(newval) {
            if (newval == 'large' || newval == 'xlarge' || newval == 'xxlarge') {
                var waitForBinding = setInterval(function () {
                    if (document.getElementById('events_contener').innerHTML.length > 0) {
                        clearInterval(waitForBinding);
                        var eventInfoConteners = document.getElementsByClassName('eventInfo');
                        for (var i = 0; i < eventInfoConteners.length; i++) {
                            if (eventInfoConteners[i].offsetLeft < 30) {
                                eventInfoConteners[i].classList.remove('large-4');
                                eventInfoConteners[i].classList.add('large-12');
                            }
                        }
                    } else {
                        clearInterval(waitForBinding);
                        var eventInfoConteners = document.getElementsByClassName('eventInfo');
                        for (var i = 0; i < eventInfoConteners.length; i++) {
                            if (eventInfoConteners[i].offsetLeft < 30) {
                                eventInfoConteners[i].classList.remove('large-4');
                                eventInfoConteners[i].classList.add('large-12');
                            }
                        }
                    }
                }, 100);
            }
        })
    }
    //});
});
