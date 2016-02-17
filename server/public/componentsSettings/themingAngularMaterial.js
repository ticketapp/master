angular.module('themingAngularMaterial', ['ngMaterial'])
    .config(function($mdThemingProvider) {
        $mdThemingProvider.definePalette('amazingPaletteName', {
            '50': 'DBDBDB',
            '100': 'DBDBDB',
            '200': 'C5D0DD',
            '300': '5D6571',
            '400': '84BDEC',
            '500': '212B3A',
            '600': '4F0029',
            '700': '4F0029',
            '800': '4F0029',
            '900': 'b71c1c',
            'A100': '84BDEC',
            'A200': 'ff5252',
            'A400': 'ff1744',
            'A700': '1A8F6F',
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