'use strict';

describe('Controller: LargeHomeCtrl', function () {

    // load the controller's module
    beforeEach(module('claudeApp'));

    var $ctrl,
        $localStorage,
        $scope,
        fakeLargeHomeFactory,
        q,
        deferred,
        $sce;

    // Initialize the controller and a mock scope

    beforeEach(inject(function ($controller, _$rootScope_, _$localStorage_, _$sce_, _$q_) {
        fakeLargeHomeFactory = {
            getInfos: function () {
                deferred = q.defer();
                // Place the fake return object here
                deferred.resolve([{
                    id: 1,
                    displayIfConnected: false,
                    animation: {content: $sce.trustAsHtml('<p style="color: black; text-align: center">' +
                        'Connectez-vous en un clique via Facebook' +
                        '<div style="position: absolute;right: -10px;height: 20px;width: 20px;background: ' +
                        'transparent;top: 20px;' +
                        'width: 0;   height: 0;   border-top: 10px solid transparent;  ' +
                        'border-bottom: 10px solid transparent;' +
                        'border-left: 10px solid white;"></div>'),
                        style : 'right: 80px;padding: 10px;' +
                            'box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);' +
                            'position: fixed;top: 230px;width: 25%;background: white;'

                    },
                    content: $sce.trustAsHtml('<h3 class="textColorWhite margin10">Connectez-vous</h3> <p>' +
                        '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
                        'Pour enrgistrer vos playlist et faire connaitre à Claude vos artistes et vos lieux favoris ' +
                        '</b>' +
                        '</p>')
                }]);
                return deferred.promise;
            }
        };
        spyOn(fakeLargeHomeFactory, 'getInfos').and.callThrough();
        $localStorage = _$localStorage_;
        $scope = _$rootScope_;
        q = _$q_;
        $sce = _$sce_;

        $ctrl = $controller('LargeHomeCtrl', {
            $scope: $scope,
            LargeHomeFactory : fakeLargeHomeFactory
        });
        $localStorage.removedInfosMsg = [];
    }));

    it('should $scope.info not be undefined', function () {
        $scope.$apply();
        expect($scope.infos).toBeDefined();
    });

    it('should inject info Id in $localStorage', function () {
        $scope.$apply();
        $scope.pushInfoIdToLocalStorage(0);
        expect($localStorage.removedInfosMsg).toEqual([1]);
    });
});