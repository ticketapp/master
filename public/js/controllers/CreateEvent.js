app.controller('CreateEventCtrl',['$scope', '$http', '$filter', '$tour', function($scope, $http, $filter, $tour){
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
	$scope.newEvent.isPublic = true;
	$scope.content ="";
	$scope.newEvent.img=[];
	$scope.newEvent.facebookId="";
	$scope.newEvent.adresses = [];
	$scope.addArt = false;
	$scope.addNewArt = [];
	$scope.content = '<div class="column large-12">ecrire ici</div>'
	$scope.imgSize = '100';
	$scope.maxlargeClass = 12;
	$scope.infoG = true;
	$scope.startTour = $tour.start;
	$scope.goTop = function () {
		window.scrollTo(0, 0);
	}
	$scope.currencyFormatting = function(value) {
		return value.toString()
	};
	$scope.testResp = function () {
		window.innerWidth = 950;
	}
	function imagePlace () {
		console.log(parseInt(document.getElementById('eventImg').style.left.match(/-?\d+/)[0]))
	}
	document.getElementById('eventImg').onmousedown = function (){
		document.addEventListener('mouseup', function () {
			if (parseInt(document.getElementById('eventImg').style.left.match(/-?\d+/)[0]) > 0) {
				document.getElementById('eventImg').style.left = 0;
			}
			if (parseInt(document.getElementById('eventImg').style.top.match(/-?\d+/)[0]) > 0) {
				document.getElementById('eventImg').style.top = 0;
			}
			if (parseInt(document.getElementById('eventImgContener').style.width.match(/-?\d+/)[0]) == 100) {
				if (parseInt(document.getElementById('eventImg').style.top.match(/-?\d+/)[0]) > 0) {
					document.getElementById('eventImg').style.top = 0;
				}
				document.getElementById('eventImg').style.left = 0;
			}
			document.getElementById('eventImgContener').removeEventListener('mousemove', imagePlace);
		});
	};
	$scope.addAnArtist = function () {
		if ($scope.addNewArt.name.length > 1) {
			$scope.addArt = false;
			$scope.newEvent.artists.push($scope.addNewArt);
			$scope.addNewArt = []
			console.log( $scope.newEvent.artists)
		}
	};
	$scope.remArt = function (i) {
		console.log(i);
		console.log($scope.newEvent.artists[i]);
		$scope.newEvent.artists.splice(i, 1);
		console.log($scope.newEvent.artists);
	};
	$scope.infoDesc = true;
	$scope.infoWy = true;
	$scope.resizeInfo = function () {
		angular.element.ready(function () {
			var eventInfoConteners = document.getElementsByClassName('eventInfo');
			for (var i = 0; i < eventInfoConteners.length; i++) {
				console.log(eventInfoConteners[i].offsetLeft)
				if (eventInfoConteners[i].offsetLeft < 30) {
					eventInfoConteners[i].classList.remove('large-4');
					eventInfoConteners[i].classList.add('large-12');
				}
			}
		});
	};
	$scope.searchEvent = function(){
		$http.get('https://graph.facebook.com/v2.2/search?q='+ $scope.eventFbName + '&limit=15&type=event&access_token=CAAUY6TFIL18BAP1ir9hDkv5EqRZCMJ1hAZA1niWapeCQUNTUXQHNF0ofKPyGN2QZBQiHwMCQOaYzMPB0KD9oGRyrdL7T0gfz6dYM5xDQ9ZC2R4aPRvE1ZBKGYozfDhGRj7Vxb2ToeZAFXBYm7ZCPaCP96aIirwpEiyWjtAJJnjnj7WY5ymc25xt0ZBYaYIs73wab1YIpCtSy8KcB8WTlmHrxv0tS6wEgyzIZD').
			success(function(data, status, headers, config) {
				console.log(data.data);
				$scope.searchEvents = data.data;
			}).
			error(function(data, status, headers, config) {

				// called asynchronously if an error occurs
				// or server returns response with an error status.
			});
	};
	$scope.GetEventById = function(id) {
		id = id.match(/\d.*/)[0];
		var scopeReady = false;
		var cover = false;
		$http.get('https://graph.facebook.com/v2.2/' + id + '/?fields=ticket_uri,id,name,start_time,owner,end_time,description,admins,venue,location&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8').
			success(function(data, status, headers, config) {
				console.log(data);
				//$scope.newEvent = data;
				var links = /((?:(http|https|Http|Https|rtsp|Rtsp):\/\/(?:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,64}(?:\:(?:[a-zA-Z0-9\$\-\_\.\+\!\*\'\(\)\,\;\?\&\=]|(?:\%[a-fA-F0-9]{2})){1,25})?\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnrwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eouw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\:\d{1,5})?)(\/(?:(?:[a-zA-Z0-9\;\/\?\:\@\&\=\#\~\-\.\+\!\*\'\(\)\,\_])|(?:\%[a-fA-F0-9]{2}))*)?(?:\b|$)/gi;;
				$scope.content = data.description.replace(/(\n\n)/g, " <br/><br/></div><div class='column large-12'>");
				$scope.content = '<div class="column large-12">' + $scope.content.replace(/(\n)/g, " <br/>") + '</div>';
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
				$scope.newEvent.facebookId = data.id;
				$scope.newEvent.place = data.location;
				$scope.newEvent.startTime = new Date(data.start_time);
				$scope.newEvent.endTime = new Date(data.end_time);
				$scope.newEvent.user = data.owner;
				$scope.newEvent.adresses.push({
					cities: data.venue.city,
					geographicPoints: data.venue.latitude + ', ' + data.venue.longitude,
					streets: data.venue.street,
					zips: data.venue.zip
				});
				$scope.newEvent.ticketSeller = data.ticket_uri;
				console.log($scope.newEvent.adresses)
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
				var img = {
					paths : data.cover.source
				};
				$scope.newEvent.img.push(img);
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
				function searchArtist (artist) {
					$http.get('/artists/contaning/'+ artist).
						success(function(data, status, headers, config) {
							//console.log(data);
							$scope.artists.push(data);
							console.log($scope.artists)
						}).
						error(function(data, status, headers, config) {
							$http.get('https://graph.facebook.com/v2.2/search?q='+ artist + '&limit=400&type=page&fields=id,category,name,link,website,likes&access_token=1434764716814175|X00ioyz2VNtML_UW6E8hztfDEZ8 ').
								success(function(data, status, headers, config) {
									$scope.data = data.data;
									var flag = 0;
									for (var i=0; i < $scope.data.length; i++) {
										if ($scope.data[i].category == 'Musician/band' || $scope.data[i].category == 'Artist') {
											for (var j=0; j < $scope.artists.length; j++) {
												if($scope.artists[j].facebookId == $scope.data[i].id) {
													flag = 1;
													break;
												}
											}
											if(flag == 0) {
												if ($scope.content.indexOf($scope.data[i].id) > -1 || $scope.content.indexOf($scope.data[i].link.replace('https://www.', '')) > -1) {
													$scope.data[i].verified = true;
													for (var iv = 0; iv < $scope.newEvent.artists.length; iv++) {
														var otherArt = $scope.newEvent.artists[iv].name.replace(/ /g, "").toLowerCase()
														var nameRefactor = $scope.data[i].name.replace(/ /g, "").toLowerCase()
														if (otherArt == nameRefactor) {
															   $scope.newEvent.artists.splice(iv, 1);
														}
													}
													if ($scope.data[i].website != undefined) {
														$scope.data[i].website = $scope.data[i].website.split(" ")
														if ($scope.data[i].website.isString) {
															console.log($scope.data[i].website)
														}
													}
													$scope.newEvent.artists.push($scope.data[i]);
													console.log($scope.newEvent.artists);
												} else if ($scope.data[i].name.toLowerCase() == artist.toLowerCase() || $scope.data[i].name.toLowerCase() + " " == artist.toLowerCase()) {
													if ($scope.newEvent.artists.length > 0 ) {
														var findArt = false;
														var notSearch = false;
														function moreLike (artToCompar, ind){
															if (notSearch == false){
																if (artToCompar.likes < $scope.data[i].likes) {
																	$scope.newEvent.artists.splice(ind, 1);
																	if ($scope.data[i].website != undefined) {
																		$scope.data[i].website = $scope.data[i].website.split(" ")
																		if ($scope.data[i].website.isString) {
																			console.log($scope.data[i].website)
																		}
																	}
																	$scope.newEvent.artists.push($scope.data[i]);
																	console.log($scope.newEvent.artists);
																}
															}
														}
														for (var ii = 0; ii < $scope.newEvent.artists.length; ii++) {
															var otherArt = $scope.newEvent.artists[ii].name.replace(/ /g, "").toLowerCase()
															var nameRefactor = $scope.data[i].name.replace(/ /g, "").toLowerCase()
															if (otherArt == nameRefactor && $scope.newEvent.artists[ii].verified == true) {
																notSearch = true;
																findArt = true;
																console.log('find')
															}
															if (otherArt == nameRefactor && $scope.newEvent.artists[ii].verified != true) {
																findArt = true;
																moreLike($scope.newEvent.artists[ii], ii)
															}
														}
														if (findArt == false) {
															$scope.newEvent.artists.push($scope.data[i]);
															console.log($scope.newEvent.artists);
														}
													} else {
														if ($scope.data[i].website != undefined) {
															$scope.data[i].website = $scope.data[i].website.split(" ")
															if ($scope.data[i].website.isString) {
																console.log($scope.data[i].website)
															}
														}
														$scope.newEvent.artists.push($scope.data[i])
														console.log($scope.newEvent.artists);
													}
												}
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
				console.log($scope.content);
				$scope.newEvent.description = $scope.content;
				$scope.eventFb = true;
				var searchArtists = $scope.newEvent.name.replace(/@.*/, "").split(/[^\S].?\W/g);
				console.log(searchArtists);
				$scope.artists = [];
				for (var i = 0; i<searchArtists.length; i++) {
					if (searchArtists[i].indexOf(' ') == 0) {
						searchArtists[i] = searchArtists[i].replace(' ', "");
					}
					searchArtist(searchArtists[i]);
				}
			} else {
				insert();
			}
		}
	};
	$scope.addImg = function () {
	  if ($scope.eventFb != true) {
		 $scope.newEvent.description = $scope.content;
	  }
	};
	$scope.clearContent = function () {
		$scope.newEvent.description = document.getElementById('content').innerHTML.replace(/contenteditable=\"true\"/g, "" );
		$scope.newEvent.description = document.getElementById('content').innerHTML.replace(/box-shadow: rgb(0, 140, 186) 0px 0px 0px 1px/g, "" );
		$scope.newEvent.description = $scope.newEvent.description.replace("ng-init=\"newEvent.description = 'ecrire ici'\"", "");
		$scope.newEvent.description = $scope.newEvent.description.replace("ng-binding", "");
		console.log($scope.newEvent)
	};

	$scope.createNewEvent = function () {
		console.log($scope.newEvent.endTime);
		for(var i = 0; i < $scope.newEvent.tarifs.length; i++) {
			$scope.newEvent.tarifs[i].startTimes = $filter('date')($scope.newEvent.tarifs[i].startTimes, "yyyy-MM-dd " +
				"HH:mm");
			$scope.newEvent.tarifs[i].endTimes = $filter('date')($scope.newEvent.tarifs[i].endTimes, "yyyy-MM-dd " +
				"HH:mm");
		}
		if ($scope.newEvent.endTime != 'Invalid Date') {
			console.log($scope.newEvent.endTime);
			$scope.newEvent.endTime = $filter('date')($scope.newEvent.endTime, "yyyy-MM-dd HH:mm")
		}
		console.log($scope.newEvent.place);
		$http.post('/events/create', {
			name: $scope.newEvent.name,
			description: $scope.newEvent.description,
			startTime: $filter('date')($scope.newEvent.startTime, "yyyy-MM-dd HH:mm"),
			endTime: $scope.newEvent.endTime,
			ageRestriction: $scope.newEvent.ageRestriction,
			images: $scope.newEvent.img,
			places: $scope.newEvent.place,
			users: $scope.newEvent.user,
			artists: $scope.newEvent.artists,
			tariffs: $scope.newEvent.tarifs,
			facebookId: $scope.newEvent.facebookId,
			isPublic: $scope.newEvent.isPublic,
			artists: $scope.artists,
			addresses: $scope.newEvent.adresses
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