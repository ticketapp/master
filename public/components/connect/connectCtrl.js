angular.module('claudeApp').controller('connectCtrl', ['$scope', '$rootScope', '$http',
    'ArtistsFactory', 'UserFactory', 'OrganizerFactory', 'EventsFactory', 'PlaceFactory',
    'StoreRequest', '$timeout', 'InfoModal', '$location', '$localStorage', 'TracksRecommender', '$filter',
    function ($scope, $rootScope, $http, ArtistsFactory, UserFactory, OrganizerFactory,
              EventsFactory, PlaceFactory, StoreRequest, $timeout, InfoModal, $location, $localStorage,
              TracksRecommender, $filter) {

        function applyLastRequest() {
            if ($rootScope.lastReq.method === 'post') {
                if ($rootScope.lastReq.object !== "") {
                    $http.post($rootScope.lastReq.path, $rootScope.lastReq.object).
                        success(function () {
                            InfoModal.displayInfo($rootScope.lastReq.success);
                            $rootScope.lastReq = {};
                        }).error(function (data, status) {
                            if (status === 401) {
                                StoreRequest.storeRequest('post',
                                    $rootScope.lastReq.path, $rootScope.lastReq.object,
                                    $rootScope.lastReq.success)
                            } else {
                                InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                            }
                        })
                } else {
                    $http.post($rootScope.lastReq.path).
                        success(function () {
                            $scope.info = $rootScope.lastReq.success;
                            $rootScope.lastReq = {};
                        }).error(function (data, status) {
                            if (status === 401) {
                                StoreRequest.storeRequest('post', $rootScope.lastReq.path, $rootScope.lastReq.object, $rootScope.lastReq.success)
                            } else {
                            }
                        })
                }
            }
        }

        function updateRemovedTracks() {
            UserFactory.getRemovedTracks().then(function (tracks) {
                if ($localStorage.tracksSignaled === undefined) {
                    $localStorage.tracksSignaled = [];
                }
                var tracksLength = tracks.length;
                for (var i = 0; i < tracksLength; i++) {
                    if ($filter('filter')($localStorage.tracksSignaled, tracks[i].trackId, 'trackId').length === 0) {
                        $localStorage.tracksSignaled.push(tracks[i].trackId)
                    }
                }
                var localStorageRemovedTracksLength = $localStorage.tracksSignaled.length;
                for (var j = 0; j < localStorageRemovedTracksLength; j++) {
                    console.log('AA',$localStorage.tracksSignaled[j]);
                    if ($filter('filter')(tracks, $localStorage.tracksSignaled[j].trackId).length === 0) {
                        console.log('BB', $localStorage.tracksSignaled);
                        TracksRecommender.UpsertTrackRate(false, $localStorage.tracksSignaled[j].trackId,
                            $localStorage.tracksSignaled[j].reason)
                    }
                }
            })
        }

        function isConnected (connectWin) {
            var waitForConnected = setInterval(function () {
                if (angular.isDefAndNotNull(connectWin.document.getElementById('top'))) {
                    clearInterval(waitForConnected);

                    if (connectWin.document.getElementById('top').getAttribute("ng-init") === '$root.connected = true') {
                        $timeout(function () {
                            $rootScope.$apply(function () {
                                $rootScope.connected = true;
                                connectWin.close();
                                InfoModal.displayInfo('Vous êtes connecté')
                            })
                        }, 0);
                        UserFactory.makeFavoriteTracksRootScope();
                        updateRemovedTracks();
                        if ($rootScope.lastReq !== {} && angular.isDefined($rootScope.lastReq)) {
                            applyLastRequest();
                        }
                    }
                }
            }, 500);
        }

        $scope.connectLink = function (url) {
            var connectWin = window.open( url, "", "toolbar=no, scrollbars=no, resizable=no, width=500, height=500");
            var changePath = setInterval(function() {
                console.log(connectWin.location.href);
                if (angular.isDefined(connectWin.location.href)) {
                    clearInterval(changePath);
                    InfoModal.displayInfo('Une erreur s\'est produite', 'error')
                }
                if (connectWin.location.href.indexOf('#/') > -1) {
                    clearInterval(changePath);
                    isConnected(connectWin);
                }
            }, 1000);
        };
    }]);