app.controller('CreateEventCtrl',['$scope', '$http', function($scope, $http){
    $scope.newEvent = [];
    //$scope.newEvent.place = [];
    $scope.newEvent.user = [];
    $scope.newEvent.artists = [];
    $scope.newEvent.tarifs = [];
    $scope.eventFb = false;
    $scope.GetEventByUrl = function(){
        $http.get('https://graph.facebook.com/v2.2/' + $scope.eventFbUrl + '/?' +  'access_token=CAACEdEose0cBAHb7fZAliRXGodDOEq2U2HLvuPFaZBXtR9GGDkpGHpcNexkT2FZAYQZBFG8GSZBiI3A5tcw6goR0wxywsaOz6NlnlQnCB2m1DIchegsTGukNflp87sFmHVatEuJU4PxWTDNXGUe6MHZCjY2C5TEPVAzIBJ4Fi37CT8KTqoLcQ3vukpngTdV4mHFZCjMbyXftuER3QVLXENO7IKb9GZC9Po0ZD').
            success(function(data, status, headers, config) {
                console.log(data);
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    };
    $scope.searchEvent = function(){
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.eventFbName + '&limit=15&type=event&access_token=CAACEdEose0cBAHb7fZAliRXGodDOEq2U2HLvuPFaZBXtR9GGDkpGHpcNexkT2FZAYQZBFG8GSZBiI3A5tcw6goR0wxywsaOz6NlnlQnCB2m1DIchegsTGukNflp87sFmHVatEuJU4PxWTDNXGUe6MHZCjY2C5TEPVAzIBJ4Fi37CT8KTqoLcQ3vukpngTdV4mHFZCjMbyXftuER3QVLXENO7IKb9GZC9Po0ZD').
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
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?' +  'access_token=CAACEdEose0cBAHb7fZAliRXGodDOEq2U2HLvuPFaZBXtR9GGDkpGHpcNexkT2FZAYQZBFG8GSZBiI3A5tcw6goR0wxywsaOz6NlnlQnCB2m1DIchegsTGukNflp87sFmHVatEuJU4PxWTDNXGUe6MHZCjY2C5TEPVAzIBJ4Fi37CT8KTqoLcQ3vukpngTdV4mHFZCjMbyXftuER3QVLXENO7IKb9GZC9Po0ZD').
            success(function(data, status, headers, config) {
                console.log(data);
                //$scope.newEvent = data;
                $scope.content = data.description.replace(/(\n\n)/g, "<br/><br/></div><div class='column large-12'>");
                $scope.content = $scope.content.replace(/(\n)/g, "<br/>");
                $scope.newEvent.name = data.name;
                $scope.newEvent.place = data.location;
                $scope.newEvent.startDate = new Date(data.start_time);
                $scope.newEvent.startTime = new Date(data.start_time);
                $scope.newEvent.endDate = new Date(data.end_time);
                $scope.newEvent.endTime = new Date(data.end_time);
                scopeReady = true;
                insert();
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?fields=cover&access_token=CAACEdEose0cBAHb7fZAliRXGodDOEq2U2HLvuPFaZBXtR9GGDkpGHpcNexkT2FZAYQZBFG8GSZBiI3A5tcw6goR0wxywsaOz6NlnlQnCB2m1DIchegsTGukNflp87sFmHVatEuJU4PxWTDNXGUe6MHZCjY2C5TEPVAzIBJ4Fi37CT8KTqoLcQ3vukpngTdV4mHFZCjMbyXftuER3QVLXENO7IKb9GZC9Po0ZD').
            success(function(data, status, headers, config) {
                $scope.newEvent.img = data.cover.source;
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
                console.log($scope.content);
                document.getElementsByTagName('iframe')[0].contentDocument.getElementById('content').getElementsByTagName('div')[0].innerHTML = '<img class="width100p" src="' + $scope.newEvent.img + '"/>' + '<div class="columns large-12"><h2>' + $scope.newEvent.name + '</h2></div>' + '<div class="columns large-12">' +  $scope.content + '</div>';
                $scope.eventFb = true;
            } else {
                insert();
            }
        }
    };
    $scope.addImg = function () {
      if ($scope.newEvent.img != null && $scope.eventFb != true) {
          document.getElementsByTagName('iframe')[0].contentDocument.getElementById('content').getElementsByTagName('div')[0].innerHTML = '<img class="width100p" src="' + $scope.newEvent.img + '"/>' + '<div class="columns large-12"><h2>' + $scope.newEvent.name + '</h2></div>'
      }
    };
    $scope.clearContent = function () {
        $scope.newEvent.description = document.getElementsByTagName('iframe')[0].contentDocument.getElementById('content').innerHTML.replace(/contenteditable=\"true\"/g, "" );
        $scope.newEvent.description = document.getElementsByTagName('iframe')[0].contentDocument.getElementById('content').innerHTML.replace(/box-shadow: rgb(0, 140, 186) 0px 0px 0px 1px/g, "" );
        $scope.newEvent.description = $scope.newEvent.description.replace("ng-init=\"event.description = 'ecrire ici'\"", "");
        $scope.newEvent.description = $scope.newEvent.description.replace("ng-binding", "");
        console.log($scope.newEvent)
    };

    $scope.createNewEvent = function () {
        $http.post('/createEvent', {name: $scope.newEvent.name,
            startSellingTime: $scope.newEvent.startDate,
            endSellingTime: $scope.newEvent.endDate,
            description: $scope.newEvent.description,
            startTime: $scope.newEvent.startTime,
            endTime: $scope.newEvent.endTime,
            ageRestriction: $scope.newEvent.ageRestriction,
            images: $scope.newEvent.img,
            places: $scope.newEvent.place,
            users: $scope.newEvent.user,
            artists: $scope.newEvent.artists,
            tariffs: $scope.newEvent.tarifs
        });
    }
}]);