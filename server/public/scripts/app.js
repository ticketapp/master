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
    'ui.sortable',
    'satellizer'
  ])
  .config(function ($routeProvider, $authProvider) {
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
            templateUrl:'assets/components/organizers/organizer.html',
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
            templateUrl:'assets/components/search/search.html',
            controller : 'searchCtrl'
        })
        .when('/iframeEvents',{
            templateUrl:'assets/components/iframe/iframe.html',
            controller: 'IframeCtrl'
        })
        .when('/connected',{
            templateUrl:'assets/views/connected.html'
        })
        .when('/mentionsLegales',{
            templateUrl:'assets/views/legales.html'
        })
      .otherwise({
        redirectTo: '/'
      });

      $authProvider.httpInterceptor = true; // Add Authorization header to HTTP request
      $authProvider.loginRedirect = '/';
      $authProvider.logoutRedirect = '/';
      //$authProvider.loginRoute = '/signIn';
      $authProvider.tokenName = 'token';
      $authProvider.tokenPrefix = 'satellizer'; // Local Storage name prefix
      $authProvider.authHeader = 'X-Auth-Token';
      $authProvider.platform = 'browser';
      $authProvider.storage = 'localStorage';

      $authProvider.facebook({
          clientId: '1434764716814175',
          authorizationEndpoint: 'https://www.facebook.com/v2.4/dialog/oauth',
          url: '/authenticate/facebook',
          scope: 'email public_profile user_likes',
          scopeDelimiter: ',',
          requiredUrlParams: ['display', 'scope'],
          display: 'popup',
          type: '2.0',
          popupOptions: { width: 481, height: 269 }
      });
  });



