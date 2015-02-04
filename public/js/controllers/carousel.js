app.controller("CarouselCtrl",function($scope, $timeout, $http){
   $scope.infos=[];
   $http.get('/infos').success(function (data, status, headers, config) {
       $scope.infos = data;
       updateInfo();
   }).error(function (data, status, headers, config) {
   });
   var i = 0 ;
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
   }
   /*$('.switch-container').mouseenter(function(){
        done = true;
     });
   $('.switch-container').mouseleave(function(){
       done = false;
   });*/
});