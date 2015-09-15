angular.module('claudeApp').factory('InfoModal', ['$modal', '$mdToast',
    function ($modal, $mdToast) {
    var factory = {
        displayInfo : function (info, type) {
            var toastPosition = {
                bottom: false,
                top: true,
                left: false,
                right: true                                                                                                                                                                                                                                                                                                                                                                                                         
            };
            var getToastPosition = function() {
                return Object.keys(toastPosition)
                    .filter(function(pos) { return toastPosition[pos]; })
                    .join(' ');
            };
            if (type == undefined || type != 'error') {
                $mdToast.show(
                    $mdToast.simple()
                        .content(info)
                        .position(getToastPosition())
                        .hideDelay(3000)
                );
            } else {

                $mdToast.show(
                    $mdToast.simple()
                        .content('une erreure s\'est produite'                                                                                                                                                                                                                                                                                                                                                                                                                                                                             )
                        .position(getToastPosition())
                        .hideDelay(6000)
                );
            }
        }
    };
    return factory;
}]);