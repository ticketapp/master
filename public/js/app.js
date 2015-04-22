var app = angular.module('MonApp',['ngAnimate',
    'ngAria',
    'ngMessages',
    'ngMaterial',
    'ngRoute',
    'ngSanitize',
    'mm.foundation',
    'colorpicker.module',
    'ngMap',
    'uiSlider',
    'ng-oboe',
    'angularytics',
    'ngWebsocket',
    'ui.sortable',
    'ngStorage'
]);

app.config(function($routeProvider, $locationProvider, AngularyticsProvider, $httpProvider){
    AngularyticsProvider.setEventHandlers(['Console', 'GoogleUniversal']);
    $httpProvider.defaults.useXDomain = true;
    delete $httpProvider.defaults.headers.common['X-Requested-With'];
    $routeProvider
    .when('/', {templateUrl: 'assets/partials/home.html'})
    .when('/events',{templateUrl: 'assets/partials/events.html'})
    .when('/event/:id',{templateUrl: 'assets/partials/eventFocus.html', controller : 'EventViewCtrl'})
    .when('/artiste/:facebookUrl',{templateUrl: 'assets/partials/artiste.html', controller : 'ArtistesCtrl'})
    .when('/user/:id',{templateUrl:'assets/partials/user.html', controller : 'UsersCtrl'})
    .when('/lieu/:id',{templateUrl:'assets/partials/user.html', controller : 'UsersCtrl'})
    .when('/createEvent',{templateUrl:'assets/partials/createEvent.html', controller : 'CreateEventCtrl'})
    .when('/search',{templateUrl:'assets/partials/search.html'})
    .when('/iframeEvents',{templateUrl:'assets/partials/iframe_calender.html', controller: 'iframeCtrl'})
    .when('/connected',{templateUrl:'assets/partials/_connected.html'})
        .otherwise('/');
}).run(function(Angularytics) {
    Angularytics.init();
});

