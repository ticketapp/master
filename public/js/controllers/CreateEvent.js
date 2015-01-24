app.controller('CreateEventCtrl',['$scope', '$http', function($scope, $http){
    $scope.event = [];
    $scope.GetEventByUrl = function(){
        $http.get('https://graph.facebook.com/v2.2/' + $scope.eventFbUrl + '/?' +  'access_token=CAACEdEose0cBAMG7RI6vyqtZCRkzkxTYvUQO0z8cjZAmkgGLmikKUy3XwfuARbl8IsZA5maVZBmRZAYOLtCO2jHFJ7UZChUhWoLimilUpbAOv5ZC85ZA0O2ZBV2tRFgNBh5Kd8ZB2ZBTXJMA8reUFRiCFC6ei6ZBvZCzQthRQwcYEFaTkBkvf6p0TU5PQFUMlUBi79xdVE669HvnGCxUdFTcxITI4zr3kryb4x18ZD').
            success(function(data, status, headers, config) {
                console.log(data);
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };
    $scope.searchEvent = function(){
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.eventFbName + '&limit=15&type=event&access_token=CAACEdEose0cBAMG7RI6vyqtZCRkzkxTYvUQO0z8cjZAmkgGLmikKUy3XwfuARbl8IsZA5maVZBmRZAYOLtCO2jHFJ7UZChUhWoLimilUpbAOv5ZC85ZA0O2ZBV2tRFgNBh5Kd8ZB2ZBTXJMA8reUFRiCFC6ei6ZBvZCzQthRQwcYEFaTkBkvf6p0TU5PQFUMlUBi79xdVE669HvnGCxUdFTcxITI4zr3kryb4x18ZD').
            success(function(data, status, headers, config) {
                console.log(data.data);

                $scope.searchEvents = data.data;
            }).
            error(function(data, status, headers, config) {

                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };
    $scope.GetEventById = function(id){
        var scopeReady = false;
        var cover = false;
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=CAACEdEose0cBAMG7RI6vyqtZCRkzkxTYvUQO0z8cjZAmkgGLmikKUy3XwfuARbl8IsZA5maVZBmRZAYOLtCO2jHFJ7UZChUhWoLimilUpbAOv5ZC85ZA0O2ZBV2tRFgNBh5Kd8ZB2ZBTXJMA8reUFRiCFC6ei6ZBvZCzQthRQwcYEFaTkBkvf6p0TU5PQFUMlUBi79xdVE669HvnGCxUdFTcxITI4zr3kryb4x18ZD').
            success(function(data, status, headers, config) {
                console.log(data);
                $scope.event = data;
                $scope.content = data.description.replace(/(\n\n)/g, "<br/><br/></div><div class='column large-12'>");
                $scope.content = $scope.content.replace(/(\n)/g, "<br/>");
                scopeReady = true;
                insert();
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?fields=cover&access_token=CAACEdEose0cBAMG7RI6vyqtZCRkzkxTYvUQO0z8cjZAmkgGLmikKUy3XwfuARbl8IsZA5maVZBmRZAYOLtCO2jHFJ7UZChUhWoLimilUpbAOv5ZC85ZA0O2ZBV2tRFgNBh5Kd8ZB2ZBTXJMA8reUFRiCFC6ei6ZBvZCzQthRQwcYEFaTkBkvf6p0TU5PQFUMlUBi79xdVE669HvnGCxUdFTcxITI4zr3kryb4x18ZD').
            success(function(data, status, headers, config) {
                $scope.event.cover = data.cover;
                console.log($scope.event.cover);
                cover = true;
                insert();
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
                cover = true;
                insert();
            });
        function insert () {
            if (scopeReady == true && cover == true) {
                //$scope.event.description.description = $scope.event.description.description.replace("↵↵", "</div><div class='column large-12' ");
                console.log($scope.content);
                document.getElementsByTagName('iframe')[0].contentDocument.getElementById('content').getElementsByTagName('div')[0].innerHTML = '<img class="width100p" src="' + $scope.event.cover.source + '"/>' + '<div class="columns large-12"><h2>' + $scope.event.name + '</h2></div>' + '<div class="columns large-12">' +  $scope.content + '</div>';
            } else {
                insert();
            }
        }
    };
    $scope.clearContent = function () {
        $scope.event.description = document.getElementsByTagName('iframe')[0].contentDocument.getElementById('content').innerHTML.replace(/contenteditable=\"true\"/g, "" );
        $scope.event.description = $scope.event.description.replace("ng-init=\"event.description = 'ecrire ici'\"", "");
        $scope.event.description = $scope.event.description.replace("ng-binding", "");
        console.log($scope.event)
    }
}]);