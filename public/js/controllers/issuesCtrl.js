app.controller('issuesCtrl', function ($scope, $http) {
   $scope.newComment= [];
   $scope.newIssue= [];
   $scope.issues = [
       {title: 'issue 1.title',
           content: 'yoyoyoyooyoyoyooyoy',
           comments: [{
               title: 'comment',
               content: 'jdhsjhjshdj'
           }]
       },
       {title: 'second title',
           content: 'fofofzk,nsnnksnknnsknsk',
           comments: [{title: 'comment', content: 'jdhsjhjshdj'}]}
   ];
    $scope.getIssues = function (issueTitle) {
        console.log(issueTitle)
    };
    $scope.addComment = function (i) {
        $scope.issues[i].comments.push($scope.newComment);
        $scope.newComment= [];
    };
    $scope.addIssue = function () {
        $scope.issues.push($scope.newIssue);
        $scope.newComment= [];
    };

});