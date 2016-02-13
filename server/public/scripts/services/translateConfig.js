angular.module("angularTranslateApp", ["pascalprecht.translate", "ngSanitize"]).config(function($translateProvider) {
    $translateProvider.translations("en", {
        HEADLINE: "Sell your concert tickets",
        INTRO_TEXT: "Get paid now, regardless of whether a buyer is found or not"
    })
    .translations("fr", {
        HEADLINE: "Vendez nous votre billet de concert",
        INTRO_TEXT: "Récuperez instantanément votre argent, sans vous soucier qu'un acheteur soit trouvé"
    });
    var userLang = (navigator.language || navigator.userLanguage).substring(0, 2);
    $translateProvider.preferredLanguage(userLang);
    $translateProvider.useSanitizeValueStrategy("sanitize");
    //All documentation: https://angularjs.de/artikel/angularjs-i18n-ng-translate
});