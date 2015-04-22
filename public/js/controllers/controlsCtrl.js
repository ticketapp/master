app.controller ('controlsCtrl', ['$scope', '$location', '$http', '$timeout', '$rootScope',
    function ($scope, $location, $http, $timeout, $rootScope ){
    $scope.buy = false;
    if ($rootScope.window != 'small') {
        var visible = true;
        function otherListener() {
            if (window.pageYOffset < 50 && visible == false) {
                visible = true;
                document.getElementById('generalControlsContener').style.marginRight = '0';
                if ( document.getElementById('infosTooltip') != undefined) {
                    var infosTooltip = document.getElementById('infosTooltip')
                    infosTooltip.classList.remove('ng-hide');
                    infosTooltip.classList.remove('fadeOut');
                    infosTooltip.classList.add('fadeIn')
                }
            } else if (window.pageYOffset > 50 && visible == true) {
                visible = false;
                document.getElementById('generalControlsContener').style.marginRight = '-60px';
                if ( document.getElementById('infosTooltip') != undefined) {
                    var infosTooltip = document.getElementById('infosTooltip')
                    infosTooltip.classList.remove('fadeIn');
                    infosTooltip.classList.add('fadeOut')
                    $timeout(function () {
                        infosTooltip.classList.add('ng-hide')
                    }, 500)
                }
            }
        }

        window.addEventListener('scroll', otherListener);
    }
    $scope.back =  function () {
        history.back();
        $scope.$apply();
    };

    $rootScope.createPlaces = function () {
        var places = [];
        var placesName =[];
        var count = 0;
        var count2 = 0;
        function getPlacesById (searchPlaces) {
            $http.get('https://graph.facebook.com/v2.2/'+ searchPlaces.id +'/?fields=checkins,cover,description,hours,id,likes,link,location,name,phone,website,picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function(data, status, headers, config) {
                    flag = 0;
                    if (data.location != undefined) {
                        if (data.location.country == undefined || data.location.country != 'France') {
                            flag = 1;
                        }
                    } else {
                        flag = 1;
                    }
                    if (flag == 0){
                        //count2 = count2 + 1;
                        //console.log("c2", count2);
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
                            for (var i=0; i < unique.length; i++) {
                                data.description = data.description.replace(new RegExp(unique[i],"g"),
                                        "<a href='" + unique[i]+ "'>" + unique[i] + "</a>")
                            }
                        }
                        function getPositionAndCreate (place) {
                            $http.get('https://maps.googleapis.com/maps/api/geocode/json?address=' +
                                place.location.street + '+' +
                                place.location.zip + '+' +
                                place.location.city + '+' +
                                place.location.country + '&key=AIzaSyDx-k7jA4V-71I90xHOXiILW3HHL0tkBYc').
                                success(function (data) {
                                    if (place.location.street == undefined) {
                                        place.location.street = '';
                                    }
                                    if (place.location.zip == undefined) {
                                        place.location.zip = '';
                                    }
                                    if (place.location.city == undefined) {
                                        place.location.city = '';
                                    }
                                    var loc = '(' + data.results[0].geometry.location.lat +
                                        ',' + data.results[0].geometry.location.lng + ')';
                                    if (place.cover != undefined) {
                                        $http.post('/places/create', {
                                            name: place.name,
                                            facebookId: place.id,
                                            geographicPoint: loc,
                                            capacity: place.checkins,
                                            description: place.description,
                                            webSite: place.website,
                                            imagePath : place.cover.source,
                                            address : {
                                                geographicPoint: loc,
                                                city: place.location.city,
                                                zip: place.location.zip,
                                                street: place.location.street
                                            }
                                        }).success(function(data){

                                        }).error(function(data){

                                        })
                                        $rootScope.resizeImgHeight();
                                    } else {
                                        $http.get('https://graph.facebook.com/v2.2/' + searchPlaces.id + '/?fields=cover, picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                                            success(function (data) {

                                                $http.post('/places/create', {
                                                    name: place.name,
                                                    facebookId: place.id,
                                                    geographicPoint: loc,
                                                    capacity: place.checkins,
                                                    description: place.description,
                                                    webSite: place.website,
                                                    imagePath: data.source,
                                                    address : {
                                                        geographicPoint: loc,
                                                        city: place.location.city,
                                                        zip: place.location.zip,
                                                        street: place.location.street
                                                    }
                                                }).success(function (data) {

                                                }).error(function (data) {

                                                })
                                                $rootScope.resizeImgHeight();
                                            }).
                                            error(function () {
                                                $http.post('/places/create', {
                                                    name: place.name,
                                                    facebookId: place.id,
                                                    geographicPoint: loc,
                                                    capacity: place.checkins,
                                                    description: place.description,
                                                    webSite: place.website,
                                                    address : {
                                                        geographicPoint: loc,
                                                        city: place.location.city,
                                                        zip: place.location.zip,
                                                        street: place.location.street
                                                    }
                                                }).success(function (data) {

                                                }).error(function (data) {

                                                })
                                            })
                                    }
                                });
                        }
                        //places.push(data);
                        getPositionAndCreate(data);
                    }
                }).
                error(function(data, status, headers, config) {

                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }
        function getPlacesByName(placeName) {
            $http.get('https://graph.facebook.com/v2.2/search?q=' + placeName + '&type=page&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function (data, status, headers, config) {
                    data = data.data;
                    for (var iv = 0; iv < data.length; iv++) {
                        if (data[iv].category == 'Concert venue' ||
                            data[iv].category == 'Club' ||
                            data[iv].category == 'Bar' ||
                            data[iv].category == 'Arts/entertainment/nightlife') {
                            getPlacesById(data[iv]);
                            //count = count + 1;
                            //
                        } else if (data[iv].category_list != undefined) {
                            for (var ii = 0; ii < data[iv].category_list.length; ii++) {
                                if (data[iv].category_list[ii].name == 'Concert Venue' ||
                                    data[iv].category_list[ii].name == 'Club' ||
                                    data[iv].category_list[ii].name == 'Bar' ||
                                    data[iv].category_list[ii].name == "Nightlife") {
                                    getPlacesById(data[iv]);
                                    //count = count + 1;
                                    //
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
                    var l = 1;                     //  set your counter to 1
                    function myLoop () {           //  create a loop function
                        setTimeout(function () {    //  call a 3s setTimeout when the loop is called
                            if (lines[l] == "Salles de 400 à 1200 places" || lines[l] == "Salles de moins de 400 places") {
                                //placesName.push(lines[l-1].replace(/ /g, "+"));
                                // console.log("places", placesName.length)
                                getPlacesByName(lines[l-1].replace(/ /g, "+"))
                            }          //  your code here
                            l++;                     //  increment the counter
                            if (l < lines.length) {            //  if the counter < 10, call the loop function
                                myLoop();             //  ..  again which will trigger another
                            }                        //  ..  setTimeout()
                        }, 50)
                    }
                    myLoop();
                }
            }
        };
        txtFile.send(null);
    }
}]);
