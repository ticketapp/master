app.controller('toolsCtrl', function ($scope, $modal, $log) {

     $scope.items = ['<a href="#/createEvent" class="btn btn-primary">Créer un évènement</a>'];
    $scope.connected;
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

app.controller('ModalInstanceCtrl', function ($scope, $modalInstance, $rootScope, items, connected) {

    $rootScope.connected = connected;
    $scope.items = items;
    $scope.selected = {
        item: $scope.items[0]
    };

    $scope.ok = function () {
        $modalInstance.close($scope.selected.item);
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});

