'use strict';

/**
 * @ngdoc overview
 * @name claudeApp
 * @description
 * # claudeApp
 *
 * Main module of the application.
 */
angular
  .module('claudeApp', [
    'ngAnimate',
    'ngAria',
    'ngCookies',
    'ngMessages',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'mm.foundation',
    'ngStorage',
    'ng-oboe',
    'ngMaterial',
    'ngMap',
    'ui.sortable'
  ])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'assets/views/main.html'
      }).when('/events/:id',{
            templateUrl: 'assets/components/events/event.html',
            controller : 'EventCtrl'
        })
        .when('/artists/:facebookUrl',{
            templateUrl: 'assets/components/artist/artist.html',
            controller : 'ArtistCtrl'
        })
        .when('/organizers/:id',{
            templateUrl:'assets/components/organizers/organizers.html',
            controller : 'OrganizerCtrl'
        })
        .when('/places/:id',{
            templateUrl:'assets/components/places/place.html',
            controller : 'PlaceCtrl'
        })
        .when('/createEvent',{
            templateUrl:'assets/components/createEvent/createEvent.html',
            controller : 'CreateEventCtrl'
        })
        .when('/search',{
            templateUrl:'assets/components/search/search.html'
        })
        .when('/iframeEvents',{
            templateUrl:'assets/components/iframe/iframe.html',
            controller: 'IframeCtrl'
        })
        .when('/connected',{
            templateUrl:'assets/views/connected.html'
        })
      .otherwise({
        redirectTo: '/'
      });
  });
