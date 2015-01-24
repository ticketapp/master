var app = angular.module('MonApp',['ngAnimate', 'ngAria', 'ngMaterial', 'ngRoute', 'ngSanitize', 'mm.foundation', 'colorpicker.module']);

app.config(function($routeProvider){
    $routeProvider
    .when('/', {templateUrl: 'assets/partials/home.html'})
    .when('/events',{templateUrl: 'assets/partials/events.html'})
    .when('/event/:id',{templateUrl: 'assets/partials/eventFocus.html', controller : 'EventViewCtrl'})
    .when('/artiste/:id',{templateUrl: 'assets/partials/artiste.html', controller : 'ArtistesCtrl'})
    .when('/user/:id',{templateUrl:'assets/partials/user.html', controller : 'UsersCtrl'})
    .when('/lieu/:id',{templateUrl:'assets/partials/lieu.html', controller : 'LieuCtrl'})
    .when('/createEvent',{templateUrl:'assets/partials/createEvent.html', controller : 'EventsCtrl'})
});

