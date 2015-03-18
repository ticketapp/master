app.controller ('controlsCtrl', ['$scope', '$location', '$http', '$timeout', '$rootScope',
    function ($scope, $location, $http, $timeout, $rootScope ){
    $scope.buy = false;
    if ($rootScope.window != 'small') {
        function otherListener() {
            if (window.pageYOffset < 50) {
                document.getElementById('generalControlsContener').style.marginRight = '0';
            } else {
                document.getElementById('generalControlsContener').style.marginRight = '-60px';
            }
        }

        window.addEventListener('scroll', otherListener);
    }
    /*function getEvent () {
        if ($location.path().indexOf('/event/') > -1) {
            matched = $location.path().match(/\d.*//*);
            /*$http.get('/events/' + matched[0])
                .success(function(data, status){
                    $scope.event = data;
                    console.log(data);
                }).error(function(data, status){
                    console.log(data);
                });
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
        }
    }
    getEvent();*/
    $scope.back =  function () {
        history.back();
        $scope.$apply();
    };

    $rootScope.createPlaces = function () {
        var places = [];
        var placeName =[];
        function getPlacesById (searchPlaces) {
            $http.get('https://graph.facebook.com/v2.2/'+ searchPlaces.id +'/?fields=checkins,cover,description,hours,id,likes,link,location,name,phone,website,picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function(data, status, headers, config) {
                    flag = 0;
                    for (m = 0; m < places.length; m++) {
                        if (places[m].id == data.id){
                            flag = 1;
                        } else if (places[m].location.latitude == data.location.latitude && places[m].location.longitude == data.location.longitude && places[m].likes > data.likes) {
                            flag = 1;
                        } else if (places[m].location.latitude == data.location.latitude && places[m].location.longitude == data.location.longitude && places[m].likes < data.likes) {
                            places.splice(m, 1);
                        }
                    }
                    if (data.location.country == undefined || data.location.country != 'France') {
                        flag = 1;
                    }
                    if (flag == 0){
                        var links = /((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)/gi;;
                        if (data.description == undefined) {
                            data.description = "";
                        }
                        data.description = data.description.replace(/(\n\n)/g, " <br/><br/></div><div class='column large-12'>");
                        data.description = data.description.replace(/(\n)/g, " <br/>");
                        if (matchedLinks = data.description.match(links)) {
                            var m = matchedLinks;
                            var unique = [];
                            for (var ii = 0; ii < m.length; ii++) {
                                var current = m[ii];
                                if (unique.indexOf(current) < 0) unique.push(current);
                            }
                            console.log(unique);
                            for (var i=0; i < unique.length; i++) {
                                data.description = data.description.replace(new RegExp(unique[i],"g"),
                                        "<a href='" + unique[i]+ "'>" + unique[i] + "</a>")
                            }
                        }
                        $http.post('/places/create', {
                            name: data.name,
                            facebookId: data.id,
                            capacity: data.checkins,
                            description:data.description,
                            webSite:data.website
                        })
                    }
                }).
                error(function(data, status, headers, config) {
                    console.log(data);
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }
        function getPlacesByName(placeName) {
            $http.get('https://graph.facebook.com/v2.2/search?q=' + placeName + '&type=page&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function (data, status, headers, config) {
                    data = data.data;
                    for (var iv = 0; iv < data.length; iv++) {
                        if (data[iv].category == 'Concert venue' || data[iv].category == 'Club') {
                            getPlacesById(data[iv]);
                        } else if (data[iv].category_list != undefined) {
                            for (var ii = 0; ii < data[iv].category_list.length; ii++) {
                                if (data[iv].category_list[ii].name == 'Concert Venue' || data[iv].category_list[ii].name == 'Club') {
                                    getPlacesById(data[iv]);
                                }
                            }
                        }
                    }
                }).
                error(function (data, status, headers, config) {
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }
        var txtFile = new XMLHttpRequest();
        txtFile.open("GET", "/assets/json/salles.txt", true);
        txtFile.onreadystatechange = function()
        {
            if (txtFile.readyState === 4) {  // document is ready to parse.
                if (txtFile.status === 200) {  // file is found
                    allText = txtFile.responseText;
                    lines = txtFile.responseText.split("\n");
                    for (var l=0; l<lines.length; l++) {
                        if (lines[l] == "Salles de 400 à 1200 places" || lines[l] == "Salles de moins de 400 places") {
                            placeName.push(lines[l-1].replace(/ /g, "+"));
                            console.log(placeName)
                            getPlacesByName(lines[l-1].replace(/ /g, "+"))
                        }
                    }
                }
            }
        };
        txtFile.send(null);
        console.log(placeName.length)
    }
}]);
