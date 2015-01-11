app.controller("CarouselCtrl",function($scope, $timeout, InfoFactory){
   $scope.infos = InfoFactory.getInfos().then(function(infos){
       $scope.infos = infos;
   }, function(msg){
       alert(msg);
   })

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
   $('.switch-container').mouseenter(function(){
        done = true;
     });
   $('.switch-container').mouseleave(function(){
       done = false;
   });
   updateInfo();
});