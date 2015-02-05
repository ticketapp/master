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
   }
    /*var searchPose = document.getElementById('events_contener').getBoundingClientRect();
    $scope.goSearch = function () {
        var scroll = setInterval(function () {
                        if (window.scrollY < searchPose.top-50) {
                            var scrollPos = window.scrollY;
                            window.scrollTo(0, scrollPos + 4);
                        } else {
                            clearInterval(scroll);
                            document.getElementById('searchTopBar').focus();
                        }
                    }, 1)
                };*/
   /*$('.switch-container').mouseenter(function(){
        done = true;
     });
   $('.switch-container').mouseleave(function(){
       done = false;
   });*/
});