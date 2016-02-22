angular.module('angularTranslateApp', ['pascalprecht.translate', 'ngSanitize']).config(function($translateProvider) {
    $translateProvider.translations('en', {
        //Landing page
        HEADLINE: 'Sell your concert tickets',
        INTRO_TEXT: "You will get paid, regardless of whether a buyer is found or not!",
        INPUT_LANDING_PAGE_TEXT: "Get an offer now",
        INPUT_LANDING_PAGE_PLACEHOLDER: "Search a salable event by name or place name",
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
        //Event
        UNTIL: "until",
        BUY_PLACE_ON: "Buy place on",
        BY: "by",
        WHO_IS_PLAYING: "Artist Lineup",
        PLAY_EVENT_PLAYLIST: "Listen the event playlist prepared by Claude",
        //Place
        UPCOMING_EVENTS: "Upcoming events",
        PAST_EVENTS: "Past events",
        //tickets
        BUY_TICKET: "Buy ticket",
        SELL_TICKET: "Sell ticket",
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
        GENERAL_TERMS: "General terms and conditions of sale"
    })
    .translations('fr', {
        //Landing page
        HEADLINE: 'Vendez nous votre billet de concert',
        INTRO_TEXT: "Vous récuperez votre argent, qu'un acheteur soit trouvé ou non !",
        INPUT_LANDING_PAGE_TEXT: "Obtenir une offre maintenant",
        INPUT_LANDING_PAGE_PLACEHOLDER: "Entrez un nom d'événement ou un lieu",
        //Events page header
        PROXIMITY_EVENTS_HEADER: "Événements à proximité",
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
        //Event
        UNTIL: "jusque",
        BUY_PLACE_ON: "Acheter des places sur",
        BY: "par",
        WHO_IS_PLAYING: "Programmation",
        PLAY_EVENT_PLAYLIST: "Écouter la playlist de l'événement préparée par Claude",
        //Place
        UPCOMING_EVENTS: "Événements à venir",
        PAST_EVENTS: "Événements passés",
        //tickets
        BUY_TICKET: "Acheter un ticket",
        SELL_TICKET: "Vendre un ticket",
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
        GENERAL_TERMS: "Conditions générales de vente"
    });

    var userLang = (navigator.language || navigator.userLanguage).substring(0, 2);
    if (userLang !== "en" && userLang !== "fr")
        userLang = "en";
    $translateProvider.preferredLanguage(userLang);
    $translateProvider.useSanitizeValueStrategy("sanitize");
    //All documentation: https://angularjs.de/artikel/angularjs-i18n-ng-translate
});