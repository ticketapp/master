angular.module('claudeApp').controller('issuesCtrl', ['$scope', '$rootScope',
    'IssuesFactory', 'InfoModal', 'StoreRequest', '$http',
    function ($scope, $rootScope, IssuesFactory, InfoModal, StoreRequest, $http) {
    $scope.newComment= [];
    $scope.newIssue= [];
    $scope.issues = [];
    $scope.viewComments = false;
    $scope.selectedIssue = false;
    $scope.issuesLimit = 10;
    $scope.loadingIssues = true;
    function getIssues () {
        IssuesFactory.getIssues().then(function (issues) {
            $scope.issues = issues;
            $scope.loadingIssues = false;
            $scope.addNewIssue = false;
        })
    }
    getIssues();
    $scope.getIssueComments = function (id) {
        IssuesFactory.getIssueComments(id).then(function (comments) {
            $scope.selectedIssue.comments = comments;
        });
    };
    $scope.addComment = function (i) {
        $http.post('/issues/' + $scope.issues[i].issueId + '/comments', {content: $scope.newComment.content}).
            success(function (data) {
                $scope.getIssueComments($scope.issues[i].issueId)
                $scope.addNewComment = false;
            }).error(function (data) {
                if (data.error == 'Credentials required') {
                    var object = {content: $scope.newComment.content};
                    StoreRequest.storeRequest('post', '/issues', object, 'votre issue' + $scope.newIssue.title + 'est enregistée');
                    $rootScope.$watch('lastReq', function (newVal) {

                        if (JSON.stringify(newVal) === '{}') {
                            getIssues()
                        }
                    })
                } else {
                    InfoModal.displayInfo('Désolé une erreur s\'est produite')
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
                    StoreRequest.storeRequest('post', '/issues', object, 'votre issue' + $scope.newIssue.title + 'est enregistée');
                    $rootScope.$watch('lastReq', function (newVal) {

                        if (JSON.stringify(newVal) === '{}') {
                            getIssues()
                        }
                    })
                } else {
                    InfoModal.displayInfo('Désolé une erreur s\'est produite')
                }
            })
    };

}]);