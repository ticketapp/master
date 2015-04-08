app.controller('issuesCtrl', function ($scope, $http, $rootScope) {
   $scope.newComment= [];
   $scope.newIssue= [];
   $scope.issues = [];
    $scope.issuesLimit = 10;
    $http.get('/issues').
        success(function(data) {
            $scope.issues = data;
        });
    $scope.getIssues = function (issueTitle) {
        console.log(issueTitle)
    };
    $scope.addComment = function (i) {
        $scope.issues[i].comments.push($scope.newComment);
        $scope.newComment= [];
    };
    $scope.addIssue = function () {
        $http.post('/issues', {content: $scope.newIssue.content, title: $scope.newIssue.title}).
            success(function(data) {
                console.log(data)
                $scope.issues.push($scope.newIssue);
                $scope.newIssue= [];
            }).
            error(function (data) {
                console.log(data)
                if (data.error == 'Credentials required') {
                    var object = {content: $scope.newIssue.content, title: $scope.newIssue.title};
                    $rootScope.storeLastReq('post', '/issues', object, 'votre issue' + $scope.newIssue.title + 'est enregistée');
                    alert('vous devez vous connecter')
                }
            })
    };

});