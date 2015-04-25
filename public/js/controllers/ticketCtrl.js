app.controller('ticketCtrl', function ($scope, $timeout) {
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