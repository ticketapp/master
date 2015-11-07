angular.module('claudeApp').factory('FollowService', ['$localStorage', 'RoutesFactory', '$q', '$http',
    function($localStorage, RoutesFactory, $q, $http) {
        var factory = {
            followedOrganizers : [],
            followedPlaces : [],
            favoritesTracks : [],
            followedEvents : [],
            followedArtists: [],
            organizers : {
                followById: function (organizerId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.organizers.followById(organizerId)).success(function(response) {
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByIdAndPushToFollowedOrganizers: function(organizer) {
                    var defered = $q.defer();
                    factory.organizers.followById(organizer.id).then(function(isFollowed, status) {
                        if (factory.followedOrganizers.length === 0) {
                            factory.organizers.followed().then(function () {
                                factory.followedOrganizers.push(organizer);
                                defered.resolve(isFollowed)
                            }, function errorCallback(error) {
                                defered.resolve(isFollowed)
                            });
                        } else {
                            factory.followedOrganizers.push(organizer);
                            defered.resolve(isFollowed)
                        }
                    }, function errorCallback(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                unfollowById: function (organizerId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.organizers.unfollowById(organizerId)).success(function(response) {
                        var organizerToRemove = factory.followedOrganizers.filter(function(organizer) {
                            return organizer.id === organizerId
                        });
                        factory.followedOrganizers.splice(factory.followedOrganizers.indexOf(organizerToRemove), 1);
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByFacebookId: function (facebookId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.organizers.followByFacebookId(facebookId)).success(function(response) {
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByFacebookIdAndPushToFollowedOrganizers: function(organizer) {
                    var defered = $q.defer();
                    factory.organizers.followByFacebookId(organizer.facebookId).then(function(isFollowed, status) {
                        if (factory.followedOrganizers.length === 0) {
                            factory.organizers.followed().then(function () {
                                factory.followedOrganizers.push(organizer);
                                defered.resolve(isFollowed)
                            }, function errorCallback(error) {
                                defered.resolve(isFollowed)
                            });
                        } else {
                            factory.followedOrganizers.push(organizer);
                            defered.resolve(isFollowed)
                        }
                    }, function errorCallback(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                isFollowed: function (organizerId) {
                    var defered = $q.defer();
                    if (factory.followedOrganizers.length > 0) {
                        var isFollowed = factory.followedOrganizers.filter(function(organizer) {
                                return organizer.id === organizerId
                            });
                        if (isFollowed.length > 0) {
                            defered.resolve(true)
                        } else { defered.resolve(false) }

                    } else {
                        $http.get(RoutesFactory.follow.organizers.isFollowed(organizerId)).success(function (response) {
                            defered.resolve(response)
                        }).error(function (error, status) {
                            defered.reject(error, status)
                        });
                    }
                    return defered.promise;
                },
                followed: function() {
                    var defered = $q.defer();
                    if (factory.followedOrganizers.length > 0) {
                        defered.resolve(factory.followedOrganizers)
                    } else {
                        $http.get(RoutesFactory.follow.organizers.followed()).success(function (organizers) {
                            factory.followedOrganizers = organizers;
                            defered.resolve(factory.followedOrganizers)
                        }).error(function (error, status) {
                            defered.reject(error, status)
                        });
                    }
                    return defered.promise;
                }
            },
            places : {
                followById: function (placeId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.places.followById(placeId)).success(function(response) {
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByIdAndPushToFollowedPlaces: function(place) {
                    var defered = $q.defer();
                    factory.places.followById(place.id).then(function(isFollowed, status) {
                        if (factory.followedPlaces.length === 0) {
                            factory.places.followed().then(function () {
                                factory.followedPlaces.push(place);
                                defered.resolve(isFollowed)
                            }, function errorCallback(error) {
                                defered.resolve(isFollowed)
                            });
                        } else {
                            factory.followedPlaces.push(place);
                            defered.resolve(isFollowed)
                        }
                    }, function errorCallback(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                unfollowById: function (placeId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.places.unfollowById(placeId)).success(function(response) {
                        var placeToRemove = factory.followedPlaces.filter(function(place) {
                            return place.id === placeId
                        });
                        factory.followedPlaces.splice(factory.followedPlaces.indexOf(placeToRemove), 1);
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByFacebookId: function (facebookId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.places.followByFacebookId(facebookId)).success(function(response) {
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByFacebookIdAndPushToFollowedPlaces: function(place) {
                    var defered = $q.defer();
                    factory.places.followByFacebookId(place.facebookId).then(function(isFollowed, status) {
                        if (factory.followedPlaces.length === 0) {
                            factory.places.followed().then(function () {
                                factory.followedPlaces.push(place);
                                defered.resolve(isFollowed)
                            }, function errorCallback(error) {
                                defered.resolve(isFollowed)
                            });
                        } else {
                            factory.followedPlaces.push(place);
                            defered.resolve(isFollowed)
                        }
                    }, function errorCallback(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                isFollowed: function (placeId) {
                    var defered = $q.defer();
                    if (factory.followedPlaces.length > 0) {
                        var isFollowed = factory.followedPlaces.filter(function(place) {
                            return place.id === placeId
                        });
                        if (isFollowed.length > 0) {
                            defered.resolve(true)
                        } else { defered.resolve(false) }

                    } else {
                        $http.get(RoutesFactory.follow.places.isFollowed(placeId)).success(function (response) {
                            defered.resolve(response)
                        }).error(function (error, status) {
                            defered.reject(error, status)
                        });
                    }
                    return defered.promise;
                },
                followed: function() {
                    var defered = $q.defer();
                    if (factory.followedPlaces.length > 0) {
                        defered.resolve(factory.followedPlaces)
                    } else {
                        $http.get(RoutesFactory.follow.places.followed()).success(function (places) {
                            factory.followedPlaces = places;
                            defered.resolve(factory.followedPlaces)
                        }).error(function (error, status) {
                            defered.reject(error, status)
                        });
                    }
                    return defered.promise;
                }
            },
            artists : {
                followById: function (artistId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.artists.followById(artistId)).success(function(response) {
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByIdAndPushToFollowedArtists: function(artist) {
                    var defered = $q.defer();
                    factory.artists.followById(artist.id).then(function(isFollowed, status) {
                        if (factory.followedArtists.length === 0) {
                            factory.artists.followed().then(function () {
                                factory.followedArtists.push(artist);
                                defered.resolve(isFollowed)
                            }, function errorCallback(error) {
                                defered.resolve(isFollowed)
                            });
                        } else {
                            factory.followedArtists.push(artist);
                            defered.resolve(isFollowed)
                        }
                    }, function errorCallback(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                unfollowById: function (artistId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.artists.unfollowById(artistId)).success(function(response) {
                        var artistToRemove = factory.followedArtists.filter(function(artist) {
                            return artist.id === artistId
                        });
                        factory.followedArtists.splice(factory.followedArtists.indexOf(artistToRemove), 1);
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByFacebookId: function (facebookId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.artists.followByFacebookId(facebookId)).success(function(response) {
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByFacebookIdAndPushToFollowedArtists: function(artist) {
                    var defered = $q.defer();
                    factory.artists.followByFacebookId(artist.facebookId).then(function(isFollowed, status) {
                        if (factory.followedArtists.length === 0) {
                            factory.artists.followed().then(function () {
                                factory.followedArtists.push(artist);
                                defered.resolve(isFollowed)
                            }, function errorCallback(error) {
                                defered.resolve(isFollowed)
                            });
                        } else {
                            factory.followedArtists.push(artist);
                            defered.resolve(isFollowed)
                        }
                    }, function errorCallback(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                isFollowed: function (artistId) {
                    var defered = $q.defer();
                    if (factory.followedArtists.length > 0) {
                        var isFollowed = factory.followedArtists.filter(function(artist) {
                            return artist.id === artistId
                        });
                        if (isFollowed.length > 0) {
                            defered.resolve(true)
                        } else { defered.resolve(false) }

                    } else {
                        $http.get(RoutesFactory.follow.artists.isFollowed(artistId)).success(function (response) {
                            defered.resolve(response)
                        }).error(function (error, status) {
                            defered.reject(error, status)
                        });
                    }
                    return defered.promise;
                },
                followed: function() {
                    var defered = $q.defer();
                    if (factory.followedArtists.length > 0) {
                        defered.resolve(factory.followedArtists)
                    } else {
                        $http.get(RoutesFactory.follow.artists.followed()).success(function (artists) {
                            factory.followedArtists = artists;
                            defered.resolve(factory.followedArtists)
                        }).error(function (error, status) {
                            defered.reject(error, status)
                        });
                    }
                    return defered.promise;
                }
            },
            tracks : {
                addToFavorites: function (trackId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.tracks.addToFavorites(trackId)).success(function(response) {
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                addToFavoriteAndAddToFactoryFavoritesTracks: function(track) {
                    var defered = $q.defer();
                    factory.tracks.addToFavorites(track.trackId).then(function(response) {
                        if (factory.favoritesTracks.length > 0) {
                            factory.favoritesTracks.push(track);
                            defered.resolve(response)
                        } else {
                            factory.tracks.favorites().then(function() {
                                factory.favoritesTracks.push(track);
                                defered.resolve(response)
                            }, function errorCallback(error) {
                                defered.resolve(response)
                            });
                        }
                    }, function errorCallback(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                removeFromFavorites: function (trackId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.tracks.removeFromFavorites(trackId)).success(function(success) {
                        var trackToRemove = factory.favoritesTracks.filter(function(track) {
                            return track.trackId === trackId
                        });
                        factory.favoritesTracks.splice(factory.favoritesTracks.indexOf(trackToRemove), 1);
                        defered.resolve(success);

                    }).error(function(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                isFollowed: function (trackId) {
                    var defered = $q.defer();
                    if (factory.favoritesTracks.length > 0) {
                        var track = factory.favoritesTracks.filter(function(track) {
                            return track.id === trackId
                        });
                        if (track.length === 0) {
                           defered.resolve(false)
                        } else { defered.resolve(true) }
                    } else {
                        $http.get(RoutesFactory.follow.tracks.isFollowed(trackId)).success(function(isFollowed) {
                            defered.resolve(isFollowed)
                        }).error(function(error) {
                            defered.reject(error)
                        })
                    }
                    return defered.promise;
                },
                favorites: function() {
                    var defered = $q.defer();
                    if (factory.favoritesTracks.length > 0) {
                        defered.resolve(factory.favoritesTracks);
                    } else {
                        $http.get(RoutesFactory.follow.tracks.favorites()).success(function (tracks) {
                            factory.favoritesTracks = tracks;
                            defered.resolve(factory.favoritesTracks)
                        }).error(function(error) {
                            defered.reject(error)
                        })
                    }
                    return defered.promise;
                }
            },
            events : {
                follow: function (eventId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.events.follow(eventId)).success(function(response) {
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                followByIdAndPushToFollowedEvents: function(event) {
                    var defered = $q.defer();
                    factory.events.follow(event.id).then(function(isFollowed, status) {
                        if (factory.followedEvents.length === 0) {
                            factory.events.followed().then(function () {
                                factory.followedEvents.push(event);
                                defered.resolve(isFollowed)
                            }, function errorCallback(error) {
                                defered.resolve(isFollowed)
                            });
                        } else {
                            factory.followedEvents.push(event);
                            defered.resolve(isFollowed)
                        }
                    }, function errorCallback(error) {
                        defered.reject(error)
                    });
                    return defered.promise;
                },
                unfollow: function (eventId) {
                    var defered = $q.defer();
                    $http.post(RoutesFactory.follow.events.unfollow(eventId)).success(function(response) {
                        var eventToRemove = factory.followedEvents.filter(function(event) {
                            return event.id === eventId
                        });
                        factory.followedEvents.splice(factory.followedEvents.indexOf(eventToRemove), 1);
                        defered.resolve(response)
                    }).error(function(error, status) {
                        defered.reject(error, status)
                    });
                    return defered.promise;
                },
                isFollowed: function (eventId) {
                    var defered = $q.defer();
                    if (factory.followedEvents.length > 0) {
                        var isFollowed = factory.followedEvents.filter(function(event) {
                            return event.id === eventId
                        });
                        if (isFollowed.length > 0) {
                            defered.resolve(true)
                        } else { defered.resolve(false) }

                    } else {
                        $http.get(RoutesFactory.follow.events.isFollowed(eventId)).success(function (response) {
                            defered.resolve(response)
                        }).error(function (error, status) {
                            defered.reject(error, status)
                        });
                    }
                    return defered.promise;
                },
                followed: function() {
                    var defered = $q.defer();
                    if (factory.followedEvents.length > 0) {
                        defered.resolve(factory.followedEvents)
                    } else {
                        $http.get(RoutesFactory.follow.events.followed()).success(function (events) {
                            factory.followedEvents = events;
                            defered.resolve(factory.followedEvents)
                        }).error(function (error, status) {
                            defered.reject(error, status)
                        });
                    }
                    return defered.promise;
                }
            }

        };
        return factory;
    }
]);