angular.module('claudeApp').controller('controlsCtrl', ['$scope', '$http', 'PlaceFactory', '$timeout', 'OrganizerFactory', 'EventsFactory',
    function ($scope, $http, PlaceFactory, $timeout, OrganizerFactory, EventsFactory) {
    $scope.addAllPlaces = function () {
	
        var token = '1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8';

        function getCoverPlace(place) {
            $http.get('https://graph.facebook.com/v2.2/' + place.id + '/?fields=cover, picture&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
                success(function (data) {
                    var newPlace = {
                        name: place.name,
                        facebookId: place.id,
                        capacity: place.checkins,
                        description: place.description,
                        webSite: place.website,
                        imagePath: data.source,
                        city: place.location.city,
                        zip: place.location.zip,
                        street: place.location.street
                    };
                    PlaceFactory.postPlace(newPlace).then(function (isCreated) {
                    })
                }).
                error(function () {
                    var newPlace = {
                        name: place.name,
                        facebookId: place.id,
                        capacity: place.checkins,
                        description: place.description,
                        webSite: place.website,
                        city: place.location.city,
                        zip: place.location.zip,
                        street: place.location.street
                    };
                    PlaceFactory.postPlace(newPlace).then(function (isCreated) {
                    })
                })
        }

        function getPositionAndCreate(place) {
            if (place.location.street == undefined) {
                place.location.street = '';
            }
            if (place.location.zip == undefined) {
                place.location.zip = '';
            }
            if (place.location.city == undefined) {
                place.location.city = '';
            }
            if (place.description == undefined) {
                place.description;
            }
            if (place.cover != undefined) {
                var newPlace = {
                    name: place.name,
                    facebookId: place.id,
                    capacity: place.checkins,
                    description: place.description,
                    webSite: place.website,
                    imagePath: place.cover.source,
                    city: place.location.city,
                    zip: place.location.zip,
                    street: place.location.street
                };
                PlaceFactory.postPlace(newPlace).then(function (isCreated) {
                    getOrganizerEvents(newPlace.facebookId)
                })
            } else {
                getCoverPlace(place);
            }
        }

        function getInfoPlace(place) {
            $http.get('https://graph.facebook.com/v2.2/' + place.id + '/?fields=checkins,cover,description,' +
                'hours,id,likes,link,location,name,phone,website,picture&access_token=' + token).
                success(function (data, status, headers, config) {
                    var flag = 0;
                    if (data.location != undefined) {
                        if (data.location.country == undefined || data.location.country != 'France') {
                            flag = 1;
                        }
                    } else {
                        flag = 1;
                    }
                    if (flag == 0) {
                        getPositionAndCreate(data);
                    }
                }).
                error(function (data, status, headers, config) {

                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }

        function getPlacePage(id) {
            $http.get('https://graph.facebook.com/v2.3/' + id + '?access_token=' + token).
                success(function (data) {
                    getInfoPlace(data)
                })
        }

	function postEventId (event) {
            EventsFactory.postEventToCreate(event.id)
        }

        function postOrganizer (organizer) {
            if (organizer.cover != undefined) {
                var newOrganizer = {};
                newOrganizer.facebookId = organizer.id;
                newOrganizer.name = organizer.name;
                newOrganizer.description = organizer.about;
                newOrganizer.websites = organizer.website;
                newOrganizer.imagePath = organizer.cover.source;
                OrganizerFactory.createOrganizer(newOrganizer).then(function (isCreated) {
                    if (isCreated != 'error') {
                        getOrganizerEvents(organizer.id);
                    }
                });
            }
        }

        function getOrganizerEvents (id) {
            $http.get('https://graph.facebook.com/v2.3/' + id + '?fields=events&access_token=' + token).
                success(function (events) {
                    if (events.events != undefined) {
                        events.events.data.forEach(postEventId)
                    }
                })
        }

        function getOrganizerPage (id) {
            $http.get('https://graph.facebook.com/v2.3/' + id + '?access_token=' + token).
                success(function (data) {
                    postOrganizer(data)
                })
        }

        function getPlacesByName(placeName) {
            $http.get('https://graph.facebook.com/v2.2/search?q=' + placeName + '&type=page&access_token=' + token).
                success(function (data, status, headers, config) {
                    data = data.data;
                    for (var iv = 0; iv < data.length; iv++) {
                        if (data[iv].category.toLowerCase() == 'concert venue' ||
                            data[iv].category.toLowerCase() == 'club' ||
                            data[iv].category.toLowerCase() == 'bar' ||
                            data[iv].category.toLowerCase() == 'arts/entertainment/nightlife') {
                            getPlacePage(data[iv].id);
                            //count = count + 1;
                            //
                        } else if (data[iv].category.toLowerCase() == 'concert tour') {
			    			getOrganizerPage(data[iv].id);
						} else if (data[iv].category_list != undefined) {
                            for (var ii = 0; ii < data[iv].category_list.length; ii++) {
                                if (data[iv].category_list[ii].name.toLowerCase() == 'concert venue' ||
                                    data[iv].category_list[ii].name.toLowerCase() == 'club' ||
                                    data[iv].category_list[ii].name.toLowerCase() == 'bar' ||
                                    data[iv].category_list[ii].name.toLowerCase() == "nightlife") {
                                    getPlacePage(data[iv].id);
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
        txtFile.open("GET", "/assets/salles.txt", true);
        txtFile.onreadystatechange = function () {
            if (txtFile.readyState === 4) {  // document is ready to parse.
                if (txtFile.status === 200) {  // file is found
                    allText = txtFile.responseText;
                    lines = txtFile.responseText.split("\n");
                    var l = 1;                     //  set your counter to 1
                    function myLoop() {           //  create a loop function
                        setTimeout(function () {    //  call a 3s setTimeout when the loop is called
                            if (lines[l] == "Salles de 400 à 1200 places" || lines[l] == "Salles de moins de 400 places") {
                                //placesName.push(lines[l-1].replace(/ /g, "+"));
                                getPlacesByName(lines[l - 1].replace(/ /g, "+"))
                            }          //  your code here
                            l++;                     //  increment the counter
                            if (l < lines.length) {            //  if the counter < 10, call the loop function
                                myLoop();             //  ..  again which will trigger another
                            }                        //  ..  setTimeout()
                        }, 500)
                    }

                    myLoop();
                }
            }
        };
        txtFile.send(null);
		var cnvList = [];
		var pageNumber = 0;
		var page = '';
		function getCnvList () {
			$http.get('https://www.cnv.fr/liste-des-affiliés' + page).success(
				function (data) {
					var listNamesPlaces = [];
					var names = data.match(/<td class="active">([^<]*)/g);
					names = names.map(function(name) {
						return name.replace('<td class="active">', '')				
					});
					if (cnvList.indexOf(names[0]) == -1) {
						cnvList = cnvList.concat(names)
						pageNumber++;
						page = '?page=' + pageNumber;
						getCnvList();
					} else {
						var i = 0
						var searchPlace = setInterval(function () {
							if (i < cnvList.length) {
								getPlacesByName(cnvList[i])
								i++
							} else {
								getPlacesByName(cnvList[i])
								clearInterval(searchPlace);
							}
						}, 200)
					}
				}).error(function(error) {
					
				});
		}
		getCnvList();
    }
}]);
