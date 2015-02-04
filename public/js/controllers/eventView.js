app.controller ('EventViewCtrl', function ($scope, EventFactory, $routeParams, $http, $timeout ){
    $http.get('/events/' + $routeParams.id)
        .success(function(data, status){
            $scope.event = data;
            console.log(data.name);
        }).error(function(data, status){
            console.log(data);
        });
    $scope.buy = false;
    $scope.generate = function() {
        $scope.buy = true;
        $timeout(function () {
            var qrcode = new QRCode(document.getElementById("qrcode"), {
                width: 100,
                height: 100
            });

            function makeCode() {
                var code = '51151515155151515';
                qrcode.makeCode(code);
            }
            makeCode();
        }, 100)
    };
    $scope.end = function () {
        $scope.buy = false;
    }
});
