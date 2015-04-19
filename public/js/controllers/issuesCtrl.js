app.controller('issuesCtrl', function ($scope, $http, $rootScope, $modal) {
    $scope.newComment= [];
    $scope.newIssue= [];
    $scope.issues = [];
    $scope.viewComments = false;
    $scope.selectedIssue = false;
    $scope.issuesLimit = 10;
    $scope.loadingIssues = true;
    function getIssues () {
        $http.get('/issues').
            success(function (data) {
                $scope.issues = data;
                $scope.loadingIssues = false;
                $scope.addNewIssue = false;
            });
    }
    getIssues();
    $scope.getIssueComments = function (id) {
        $http.get('/issues/'+ id + '/comments ').
            success(function (data) {
                $scope.selectedIssue.comments = data
            }).
            error(function (data) {
                
            })
    }
    $scope.addComment = function (i) {
        $http.post('/issues/' + $scope.issues[i].issueId + '/comments', {content: $scope.newComment.content}).
            success(function (data) {
                $scope.getIssueComments($scope.issues[i].issueId)
                $scope.addNewComment = false;
            }).error(function (data) {
                if (data.error == 'Credentials required') {
                    var object = {content: $scope.newComment.content};
                    $rootScope.storeLastReq('post', '/issues', object, 'votre issue' + $scope.newIssue.title + 'est enregistée');
                    $rootScope.$watch('lastReq', function (newVal) {
                        
                        if (JSON.stringify(newVal) === '{}') {
                            getIssues()
                        }
                    })
                } else {
                    $scope.info = 'Désolé une erreur s\'est produite';
                    var modalInstance = $modal.open({
                        templateUrl: 'assets/partials/_infoModal.html',
                        controller: 'infoModalCtrl',
                        resolve: {
                            info: function () {
                                return $scope.info;
                            }
                        }
                    });
                    modalInstance.result.then(function () {
                        $log.info('Modal dismissed at: ' + new Date());
                    });
                }
            });
        $scope.newComment= [];
    };
    $scope.addIssue = function () {
        $http.post('/issues', {content: $scope.newIssue.content, title: $scope.newIssue.title}).
            success(function(data) {
                
                $scope.newIssue= [];
                $scope.addNewIssue = false;
                getIssues();
            }).
            error(function (data) {
                if (data.error == 'Credentials required') {
                    var object = {content: $scope.newIssue.content, title: $scope.newIssue.title};
                    $rootScope.storeLastReq('post', '/issues', object, 'votre issue' + $scope.newIssue.title + 'est enregistée');
                    $rootScope.$watch('lastReq', function (newVal) {
                        
                        if (JSON.stringify(newVal) === '{}') {
                            getIssues()
                        }
                    })
                } else {
                    $scope.info = 'Désolé une erreur s\'est produite';
                    var modalInstance = $modal.open({
                        templateUrl: 'assets/partials/_infoModal.html',
                        controller: 'infoModalCtrl',
                        resolve: {
                            info: function () {
                                return $scope.info;
                            }
                        }
                    });
                    modalInstance.result.then(function () {
                        $log.info('Modal dismissed at: ' + new Date());
                    });
                }
            })
    };

});