var app = angular.module('MonApp',['ngAnimate', 'ngAria', 'ngMessages', 'ngMaterial', 'ngRoute', 'ngSanitize', 'mm.foundation', 'colorpicker.module', 'ngMap', 'uiSlider', 'ng-oboe', 'angularytics', 'ngWebsocket']);

app.config(function($routeProvider, $locationProvider, AngularyticsProvider){
    AngularyticsProvider.setEventHandlers(['Console', 'GoogleUniversal']);
    $routeProvider
    .when('/', {templateUrl: 'assets/partials/home.html'})
    .when('/events',{templateUrl: 'assets/partials/events.html'})
    .when('/event/:id',{templateUrl: 'assets/partials/eventFocus.html', controller : 'EventViewCtrl'})
    .when('/artiste/:facebookUrl',{templateUrl: 'assets/partials/artiste.html', controller : 'ArtistesCtrl'})
    .when('/user/:id',{templateUrl:'assets/partials/user.html', controller : 'UsersCtrl'})
    .when('/lieu/:id',{templateUrl:'assets/partials/lieu.html', controller : 'PlaceCtrl'})
    .when('/createEvent',{templateUrl:'assets/partials/createEvent.html', controller : 'CreateEventCtrl'})
    .when('/search',{templateUrl:'assets/partials/search.html'})
        .otherwise('/');
}).run(function(Angularytics) {
    Angularytics.init();
});

