'use strict';
angular.module('claudeApp')
    .directive('header', function () {
        return {
            controller: 'headerCtrl',
            restrict : 'E',
            templateUrl : 'assets/components/header/header.html'
        }
    });