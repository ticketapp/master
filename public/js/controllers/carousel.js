app.controller("CarouselCtrl",function($scope, $timeout, $http, $sce, $rootScope){
   $scope.infos=[];
   $http.get('/infos').success(function (data, status, headers, config) {
       $scope.infos = data;
       if ($rootScope.window == 'large') {
           $scope.infos.push({title: '', content: $sce.trustAsHtml('<iframe src="http://localhost:9000/" height="350px" width="25%"></iframe><span class="column large-8">Test smartPhone</span>')})
       }
       updateInfo();
   }).error(function (data, status, headers, config) {
   });
   var i = -1 ;
    var changeInf;
   function updateInfo(){
       if(i === $scope.infos.length - 1){
           i = 0;
       } else {
           i++;
       }
       $scope.elementEnCours = $scope.infos[i];
       changeInf = $timeout(updateInfo,8000);
       document.getElementsByClassName('parallax-background')[0].onmouseover = function(){
           $timeout.cancel(changeInf);
       };
       document.getElementsByClassName('parallax-background')[0].onmouseleave = function(){
           updateInfo();
       };
   }/*
   document.getElementsByClassName('parallax-background')[0].onmouseover = function(){
        done = true;
       console.log('yo')
     };
   document.getElementsByClassName('parallax-background')[0].onmouseleave = function(){
       done = false;
       console.log('yoyo')
   };*/
});