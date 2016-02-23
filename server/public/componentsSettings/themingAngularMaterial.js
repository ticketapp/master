angular.module('themingAngularMaterial', ['ngMaterial'])
    .config(function($mdThemingProvider) {
        $mdThemingProvider.definePalette('amazingPaletteName', {
            '50': 'DBDBDB',
            '100': 'DBDBDB',
            '200': 'C5D0DD',
            '300': '5D6571',
            '400': '84BDEC',
            '500': '212B3A',
            '600': '2C5773',
            '700': '038D98',
            '800': 'AFC2DF',
            '900': 'CEE8E7',
            'A100': '84BDEC',
            'A200': 'BDCFE8',
            'A400': 'B6BFCB',
            'A700': '68A5FF',
            'contrastDefaultColor': 'light',    // whether, by default, text (contrast)
                                                // on this palette should be dark or light
            'contrastDarkColors': ['50', '100', //hues which contrast should be 'dark' by default
                '200', '300', '400', 'A100'],
            'contrastLightColors': undefined    // could also specify this if default was 'dark'
        });
        $mdThemingProvider.theme('default')
            .primaryPalette('amazingPaletteName', {
                'default': '400', // by default use shade 400 from the pink palette for primary intentions
                'hue-1': '100', // use shade 100 for the <code>md-hue-1</code> class
                'hue-2': '500', // use shade 600 for the <code>md-hue-2</code> class
                'hue-3': 'A100' // use shade A100 for the <code>md-hue-3</code> class
            });
    });
