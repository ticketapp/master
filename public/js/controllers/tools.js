app.controller('toolsCtrl', function ($scope, $modal, $log) {

     $scope.items = ['<a href="#/createEvent" class="btn btn-primary"><i class="icon-white icon-edit"></i></a><h4>Créer un évènement</h4>'];

     $scope.open = function (size) {

       var modalInstance = $modal.open({
         templateUrl: 'assets/partials/_tools.html',
         controller: 'ModalInstanceCtrl',
         size: size,
         resolve: {
           items: function () {
             return $scope.items;
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

   angular.module('ui.bootstrap').controller('ModalInstanceCtrl', function ($scope, $modalInstance, items) {

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
