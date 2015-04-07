app.controller('toolsCtrl', function ($scope, $modal, $log, $rootScope) {
    //$scope.items = [];
    $scope.connected = $rootScope.connected;
    console.log($scope.connected)
    $scope.open = function () {
        var modalInstance = $modal.open({
            templateUrl: 'assets/partials/_tools.html',
            controller: 'ModalInstanceCtrl',
            resolve: {
                items: function () {
                    return $scope.items;
                },
                connected: function () {
                    return $scope.connected;
                }
            }
        });

        modalInstance.result.then(function (selectedItem) {
            $scope.selected = selectedItem;
        }, function () {
            $log.info('Modal dismissed at: ' + new Date());
        });
    };
});

// Please note that $modalInstance represents a modal window (instance) dependency.
// It is not the same as the $modal service used above.

app.controller('ModalInstanceCtrl', function ($scope, $modalInstance, $rootScope, items, connected, $http) {
    $scope.items = items;
    $scope.logout = function () {
        $http.get('/logout').
            success(function (data) {
                console.log(data)
                $rootScope.connected = false;
                alert('vous êtes deconnecté')
            })
    };
    $scope.getPlaylists = function() {
        $http.get('/playlists').
            success(function(data) {
                $scope.playlists = data;
                console.log(data)
            })
    };
    $scope.ok = function () {
        $modalInstance.close();
    };
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});

