angular.module('angularTranslateApp', ['pascalprecht.translate', 'ngSanitize']).config(function($translateProvider) {
    $translateProvider.translations('en', {
        //Landing page
        HEADLINE: 'Sell your concert tickets',
        INTRO_TEXT: "Get paid now, regardless of whether a buyer is found or not",
        INPUT_LANDING_PAGE_TEXT: "Get an offer now",
        INPUT_LANDING_PAGE_PLACEHOLDER: "Search a salable event by name or place name",
        KNOW_MORE: "Know more",
        //Events page header
        PROXIMITY_EVENTS_HEADER: "Events near you",
        //Events forms
        ID_FORM_EVENTS_HEADER: "Search by id",
        PROXIMITY_EVENTS_FORM_HEADER: "Find events by geolocation",
        HOUR_INTERVAL_EVENTS_FORM_HEADER: "Find events by date",
        PASSED_EVENTS_FORM_HEADER: "Find events passed by date",
        EVENTS_CONTAINING_FORM_HEADER: "Find all containing",
        EVENTS_BY_CITY_FORM_HEADER: "Find in city",
        EVENTS_NEAR_CITY_FORM_HEADER: "Find near city",
        CREATE_EVENT_BY_FACEBOOK_ID_FORM_HEADER: "Create by facebook id",
        ADVANCED_SEARCH_HEADER: "Advanced search",
        //tickets
        BUY_TICKET: "Buy ticket",
        SELL_TICKET: "Sell ticket",
        SELL_TICKET_DESCRIPTION: "Claude vous propose un prix de rachat pour votre billet. Ouvrez un compte en quelque clic," +
        " et claude vous rachette directement votre billet.",
        SELL_TICKET_TITLE_1: "how does it work ?",
        SELL_TICKET_TITLE: "Sell your ticket for: ",
        //chatContact
        START_CONVERSATION: "Start a conversation",
        CLAUDE_TEAM: "Claude Team",
        SMALL_CHAT_HOME_MESSAGE: "Ask us anything or share your feedback. We'd be happy to hear what you think about " +
            "Claude.",
        CHAT_HOME_MESSAGE_IF_CONNECTED: "We are connected, ask us anything. We'd be glad to hear what brought you to " +
            "Claude!",
        CHAT_HOME_MESSAGE_IF_NOT_CONNECTED: "We are not connected but enter your email in order to get notified. We " +
            "always respond the same day!",
        EMAIL_PLACE_HOLDER: "Your email",
        //footer
        LEGAL_NOTICES: "Legal notices",
        GENERAL_TERMS: "General terms and conditions of sale",
        //legals
        COOKIES: "En poursuivant votre navigation sur ce site, vous acceptez l'utilisation de cookies pour réaliser " +
        "des statistiques de visites."
    })
    .translations('fr', {
        //Landing page
        HEADLINE: 'Vendez nous votre billet de concert',
        INTRO_TEXT: "Récuperez instantanément votre argent, sans vous soucier qu'un acheteur soit trouvé",
        INPUT_LANDING_PAGE_TEXT: "Obtenir une offre maintenant",
        INPUT_LANDING_PAGE_PLACEHOLDER: "Entrez un nom d'événement ou un lieu",
        KNOW_MORE: "En savoir plus",
        //Events page header
        PROXIMITY_EVENTS_HEADER: "Evénements à proximité",
        //Events forms
        ID_FORM_EVENTS_HEADER: "Rechercher par id",
        PROXIMITY_EVENTS_FORM_HEADER: "Rechercher par coordonnées géographiques",
        HOUR_INTERVAL_EVENTS_FORM_HEADER: "Rechercher par date",
        PASSED_EVENTS_FORM_HEADER: "Rechercher les evénements passés par date",
        EVENTS_CONTAINING_FORM_HEADER: "Rechercher par nom",
        EVENTS_BY_CITY_FORM_HEADER: "Rechercher les événements d'une ville",
        EVENTS_NEAR_CITY_FORM_HEADER: "Rechercher les événements proche d'une ville",
        CREATE_EVENT_BY_FACEBOOK_ID_FORM_HEADER: "Créer un événement par id facebook",
        ADVANCED_SEARCH_HEADER: "Recherche avancée",
        //tickets
        BUY_TICKET: "Acheter un ticket",
        SELL_TICKET: "Vendre un ticket",
        SELL_TICKET_DESCRIPTION: "Claude vous propose un prix de rachat pour votre billet. Ouvrez un compte en quelque clic," +
            " et claude vous rachette directement votre billet.",
        SELL_TICKET_TITLE_1: "Comment ça marche ?",
        SELL_TICKET_TITLE: "Vendez votre billet pour: ",
        //chatContact
        START_CONVERSATION: "Démarrer une conversation",
        CLAUDE_TEAM: "L'équipe Claude",
        SMALL_CHAT_HOME_MESSAGE: "N'hésitez pas à nous demander quoi que ce soit ou à partager vos critiques.",
        CHAT_HOME_MESSAGE_IF_CONNECTED: "Nous sommes connectés, n'hésitez pas à nous demander quoi que ce soit !",
        CHAT_HOME_MESSAGE_IF_NOT_CONNECTED: "Nous ne sommes pas connectés, mais laissez nous votre email, nous " +
            "répondons toujours le jour même !",
        EMAIL_PLACE_HOLDER: "Votre email",
        //footer
        LEGAL_NOTICES: "Mentions légales",
        GENERAL_TERMS: "Conditions générales de vente",
        //legals
        COOKIES: "En poursuivant votre navigation sur ce site, vous acceptez l'utilisation de cookies pour réaliser " +
        "des statistiques de visites."
    });

    var userLang = (navigator.language || navigator.userLanguage).substring(0, 2);
    if (userLang !== "en" && userLang !== "fr")
        userLang = "en";
    $translateProvider.preferredLanguage(userLang);
    $translateProvider.useSanitizeValueStrategy("sanitize");
    //All documentation: https://angularjs.de/artikel/angularjs-i18n-ng-translate
});