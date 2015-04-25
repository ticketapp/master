app.controller ('PlaceCtrl', function ($scope, $http, $routeParams ){
    function imgHeight () {
        var waitForContentMin = setTimeout(function () {
            var content = document.getElementsByClassName('img_min_evnt');
            if (content.length > 0) {
                clearInterval(waitForContentMin);
                var newHeight = content[0].clientWidth * 0.376 + 'px';
                for (var i = 0; i < content.length; i++) {
                    content[i].style.height = newHeight;
                }
            }
        }, 100)
    }
    $http.get('/places/'+ $routeParams.id).
        success(function (data, status, headers, config){
            $scope.place = data;
        });
    $http.get('/places/'+ $routeParams.id + '/events').
        success(function (data, status, headers, config){
            $scope.events = data;
            imgHeight()
        })
});
