app.controller('issuesCtrl', function ($scope, $http) {
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
        $scope.issues.push($scope.newIssue);
        console.log($scope.newIssue)
        $http.post('/issues', {content: $scope.newIssue.content, title: $scope.newIssue.title}).
            success(function(data) {
                console.log(data)
                $scope.newIssue= [];
            }).
            error(function (data) {
                console.log(data)
            })
    };

});