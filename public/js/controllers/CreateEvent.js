app.controller('CreateEventCtrl',['$scope', '$http', '$filter', function($scope, $http, $filter){
    $scope.newEvent = [];
    //$scope.newEvent.place = [];
    $scope.newEvent.user = [];
    $scope.newEvent.artists = [];
    $scope.newEvent.tarifs = [];
    $scope.eventFb = false;
    $scope.newEvent.startTime = "";
    $scope.newEvent.endTime = "";
    $scope.newEvent.name = "";
    $scope.newEvent.place = "";
    $scope.newEvent.description = "";
    $scope.newEvent.ageRestriction = 16;
    $scope.searchEvent = function(){
        $http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.eventFbName + '&limit=15&type=event&access_token=CAAUY6TFIL18BAGjacxgHOsfwWkZBKBVW3nD7BULQgV53qScUZAQWSkGqu4DLmFhBwnHNBLSTXKuZAPprqKEqigGR4w5CZA3jgP36qf0ZAgOJAn9KufsiNZAZAzQewEm1WZA717ue3H4arO4zZBoKfZCQla4KIxqqWhLDif5WxZABdBDrRZCZClICkqhTPWFobmERLuaZCFiPEJKJaokQCSTRVOfsSEQ9NDoWPJWT8ZD').
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
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
            success(function(data, status, headers, config) {
                console.log(data);
                //$scope.newEvent = data;
                var links = /((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)/gi;;
                $scope.content = data.description.replace(/(\n\n)/g, " <br/><br/></div><div class='column large-12'>");
                $scope.content = $scope.content.replace(/(\n)/g, " <br/>");
                if (matchedLinks = $scope.content.match(links)) {
                    var m = matchedLinks;
                    var unique = [];
                    for (var ii = 0; ii < m.length; ii++) {
                        var current = m[ii];
                        if (unique.indexOf(current) < 0) unique.push(current);
                    }
                    console.log(unique);
                    for (var i=0; i < unique.length; i++) {
                        $scope.content = $scope.content.replace(new RegExp(unique[i],"g"),
                                "<a href='" + unique[i]+ "'>" + unique[i] + "</a>")
                    }
                }
                $scope.newEvent.name = data.name;
                $scope.newEvent.place = data.location;
                $scope.newEvent.startTime = new Date(data.start_time);
                $scope.newEvent.endTime = new Date(data.end_time);
                scopeReady = true;
                insert();
                console.log($scope.newEvent.startDate)
            }).
            error(function(data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
        $http.get('https://graph.facebook.com/v2.2/' + id + '/?fields=cover&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
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
                var searchArtists = $scope.newEvent.name.replace(/@.*/, "").split(/[^\S]\W/g);
                console.log(searchArtists);
                $scope.artists = [];
                $scope.artistesFb = [];
                for (var i = 0; i<searchArtists.length; i++) {
                    searchArtist(searchArtists[i]);
                }
                function searchArtist (artist) {
                    $http.get('/artists/contaning/'+ artist).
                        success(function(data, status, headers, config) {
                            //console.log(data);
                            $scope.artists.push(data);
                            console.log($scope.artists)
                        }).
                        error(function(data, status, headers, config) {
                            $http.get('https://graph.facebook.com/v2.2/search?q='+ artist + '&limit=200&type=page&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8 ').
                                success(function(data, status, headers, config) {
                                    $scope.data = data.data;
                                    var flag = 0;
                                    console.log($scope.data);
                                    for (var i=0; i < $scope.data.length; i++) {
                                        if ($scope.data[i].category == 'Musician/band') {
                                            for (var j=0; j < $scope.artists.length; j++) {
                                                if($scope.artistes[j].facebookId == $scope.data[i].id) {
                                                    flag = 1;
                                                    break;
                                                }
                                            }
                                            if(flag == 0) {
                                                $scope.artistesFb.push($scope.data[i]);
                                                console.log($scope.artistesFb);
                                            } else {
                                                flag = 0;
                                            }
                                        }
                                    }

                                }).
                                error(function(data, status, headers, config) {
                                    // called asynchronously if an error occurs
                                    // or server returns response with an error status.
                                });
                        });
                }
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
        
        console.log($scope.newEvent.startTime);
        //$scope.newEvent.startDate = $scope.newEvent.startDate.getFullYear() + '-' + $scope.newEvent.startDate.getMonth()+1 + '-' + $scope.newEvent.startDate.getDate();
        //$scope.newEvent.endDate = $scope.newEvent.endDate.getFullYear() + '-' + $scope.newEvent.endDate.getMonth()+1 + '-' + $scope.newEvent.endDate.getDate();
        //$scope.newEvent.startTime = $scope.newEvent.startTime.getFullYear() + '-' + $scope.newEvent.startTime.getMonth()+1 + '-' + $scope.newEvent.startTime.getDate();
        //$scope.newEvent.endTime = $scope.newEvent.endTime.getFullYear() + '-' + $scope.newEvent.endTime.getMonth()+1 + '-' + $scope.newEvent.endTime.getDate();
        $http.post('/events/create', {
            name: $scope.newEvent.name,
            //startSellingTime: $scope.newEvent.startDate.getFullYear() + '-' + $scope.newEvent.startDate.getMonth()+1 + '-' + $scope.newEvent.startDate.getDate(),
            //endSellingTime: $scope.newEvent.endDate.getFullYear() + '-' + $scope.newEvent.endDate.getMonth()+1 + '-' + $scope.newEvent.endDate.getDate(),
            description: $scope.newEvent.description,
            startTime: $filter('date')($scope.newEvent.startTime, "yyyy-MM-dd HH:mm"),
            endTime: $filter('date')($scope.newEvent.endTime, "yyyy-MM-dd HH:mm"),
            ageRestriction: $scope.newEvent.ageRestriction,
            images: $scope.newEvent.img,
            places: $scope.newEvent.place,
            users: $scope.newEvent.user,
            artists: $scope.newEvent.artists,
            tariffs: $scope.newEvent.tarifs
        }).
            success(function(data, status, headers, config) {
                window.location.href =('#/event/' + data.eventId);
                console.log(data)
            }).
            error(function(data, status, headers, config) {
                console.log(data)
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    }
}]);