angular.module('angularTranslateApp', ['pascalprecht.translate', 'ngSanitize']).config(function($translateProvider) {
    $translateProvider.translations('en', {
        //Landing page
        HEADLINE: 'Sell your tickets',
        INTRO_TEXT: "Be payed now, claude is the buyer",
        INPUT_LANDING_PAGE_TEXT: "Get an offer now",
        INPUT_LANDING_PAGE_PLACEHOLDER: "Search a sellableEvent by name or placeName",
        //Events page header
        PROXIMITY_EVENTS_HEADER: "Events near you",
        //Events forms
        ID_FORM_EVENTS_HEADER: "Search by id",
        PROXIMITY_EVENTS_FORM_HEADER: "Find events by geolocation",
        HOUR_INTERVAL_EVENTS_FORM_HEADER: "Find events In Hour Interval",
        PASSED_EVENTS_FORM_HEADER: "Find events Passed In Interval",
        EVENTS_CONTAINING_FORM_HEADER: "Find All Containing",
        EVENTS_BY_CITY_FORM_HEADER: "Find in city",
        EVENTS_NEAR_CITY_FORM_HEADER: "Find near city",
        CREATE_EVENT_BY_FACEBOOK_ID_FORM_HEADER: "Create by facebook id",
        ADVANCED_SEARCH_HEADER: "Advanced search"
    })
    .translations('fr', {
        //Landing page
        HEADLINE: 'Vendez nous votre billet de concert',
        INTRO_TEXT: "Récuperez instantanément votre argent, sans vous soucier qu'un acheteur soit trouvé",
        INPUT_LANDING_PAGE_TEXT: "Obtenir une offre maintenant",
        INPUT_LANDING_PAGE_PLACEHOLDER: "Entrez un nom d'événement ou un lieu",
        //Events page header
        PROXIMITY_EVENTS_HEADER: "Evénements à proximité",
        //Events forms
        ID_FORM_EVENTS_HEADER: "Rechercher par id",
        PROXIMITY_EVENTS_FORM_HEADER: "Rechercher par coordonnées géographique",
        HOUR_INTERVAL_EVENTS_FORM_HEADER: "Rechercher par temps",
        PASSED_EVENTS_FORM_HEADER: "Rechercher les evénements passés",
        EVENTS_CONTAINING_FORM_HEADER: "Rechercher par nom",
        EVENTS_BY_CITY_FORM_HEADER: "Rechercher les événements d'une ville",
        EVENTS_NEAR_CITY_FORM_HEADER: "Rechercher les événements proche d'une ville",
        CREATE_EVENT_BY_FACEBOOK_ID_FORM_HEADER: "Créer un événement par id facebook",
        ADVANCED_SEARCH_HEADER: "Recherche avancée"
    });
    var userLang = (navigator.language || navigator.userLanguage).substring(0, 2);
    $translateProvider.preferredLanguage(userLang);
    $translateProvider.useSanitizeValueStrategy('sanitize');
    //All documentation: https://angularjs.de/artikel/angularjs-i18n-ng-translate
});