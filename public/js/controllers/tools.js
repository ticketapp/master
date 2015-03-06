app.controller('toolsCtrl', function ($scope, $modal, $log, $rootScope) {
    $scope.items = ['<a href="#/createEvent" class="btn btn-primary">Créer un évènement</a>', '<a class="button" href="/logout">Logout</a>'];
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
    $scope.ok = function () {
        $modalInstance.close();
    };
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});

