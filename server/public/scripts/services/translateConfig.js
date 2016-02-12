angular.module('angularTranslateApp', ['pascalprecht.translate', 'ngSanitize']).config(function($translateProvider) {
    $translateProvider.translations('en', {
        HEADLINE: 'Sell your tickets',
        INTRO_TEXT: "be payed know, claude is the buyer"
    })
    .translations('fr', {
        HEADLINE: 'Vendez nous votre billet de concert',
        INTRO_TEXT: "Récuperez instantanément votre argent, sans vous soucier qu'un acheteur soit trouvé"
    });
    var userLang = (navigator.language || navigator.userLanguage).substring(0, 2);
    $translateProvider.preferredLanguage(userLang);
    $translateProvider.useSanitizeValueStrategy('sanitize');
    //All documentation: https://angularjs.de/artikel/angularjs-i18n-ng-translate
});