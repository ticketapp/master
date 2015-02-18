var app = angular.module('MonApp',['ngAnimate', 'ngAria', 'ngMessages', 'ngMaterial', 'ngRoute', 'ngSanitize', 'mm.foundation', 'colorpicker.module', 'ngMap']);

app.config(function($routeProvider, $locationProvider){
    $routeProvider
    .when('/', {templateUrl: 'assets/partials/home.html'})
    .when('/events',{templateUrl: 'assets/partials/events.html'})
    .when('/event/:id',{templateUrl: 'assets/partials/eventFocus.html', controller : 'EventViewCtrl'})
    .when('/artiste/:id',{templateUrl: 'assets/partials/artiste.html', controller : 'ArtistesCtrl'})
    .when('/user/:id',{templateUrl:'assets/partials/user.html', controller : 'UsersCtrl'})
    .when('/lieu/:id',{templateUrl:'assets/partials/lieu.html', controller : 'PlaceCtrl'})
    .when('/createEvent',{templateUrl:'assets/partials/createEvent.html', controller : 'CreateEventCtrl'})
    .when('/search',{templateUrl:'assets/partials/search.html'})
        .otherwise('/');
});

