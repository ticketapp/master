app.controller("CarouselCtrl",function($scope, $timeout, $http){
   $scope.infos=[];
   $http.get('/infos').success(function (data, status, headers, config) {
       $scope.infos = data;
       updateInfo();
   }).error(function (data, status, headers, config) {
   });
   var i = -1 ;
   var done = false;
   function updateInfo(){
       if (!done) {
           if(i === $scope.infos.length - 1){
               i = 0;
           } else {
               i++;
           }
           $scope.elementEnCours = $scope.infos[i];
           $timeout(updateInfo,8000);
      }
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