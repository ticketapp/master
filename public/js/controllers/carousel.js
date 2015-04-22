app.controller("CarouselCtrl",function($scope, $timeout, $http, $sce, $compile){
   $scope.infos=[];
   $http.get('/infos').success(function (data, status, headers, config) {
       //$scope.infos = data;
       $scope.infos.push({
           animation: {content: $sce.trustAsHtml('<p style="color: black; text-align: center">vous pouvez ' +
               'trouver la rubrique FAQ/rapporter un bug ici.' +
                   '<div style="position: absolute;right: -10px;height: 20px;width: 20px;background: ' +
                   'transparent;top: 20px;' +
                   'width: 0;   height: 0;   border-top: 10px solid transparent;  ' +
                   'border-bottom: 10px solid transparent;' +
                   'border-left: 10px solid white;"></div>'),
                   style : 'right: 80px;padding: 10px;' +
                       'box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);' +
                       'position: fixed;top: 170px;width: 25%;background: white;'

               },
           content: $sce.trustAsHtml('<h2 class="text-center textColorWhite margin10">Bienvenue</h2><p>' +
                '<b class="column large-6 large-offset-3 text-center textColorWhite medium-11">' +
                'Claude est en version beta, aidez le à s\'ammélliorez en rapportant vos bug ou en ' +
                'partageant vos suggestions.' +
                '</b>' +
                '</p>')
       });
       $scope.infos.push({
           animation: {content: $sce.trustAsHtml('<p style="color: black; text-align: center">' +
               'Connectez-vous en un clique via Facebook' +
                   '<div style="position: absolute;right: -10px;height: 20px;width: 20px;background: ' +
                   'transparent;top: 20px;' +
                   'width: 0;   height: 0;   border-top: 10px solid transparent;  ' +
                   'border-bottom: 10px solid transparent;' +
                   'border-left: 10px solid white;"></div>'),
                   style : 'right: 80px;padding: 10px;' +
                       'box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.4);' +
                       'position: fixed;top: 230px;width: 25%;background: white;'

               },
           content: $sce.trustAsHtml('<h3 class="textColorWhite margin10">Connectez-vous</h3> <p>' +
                '<b class="column large-6 large-offset-3 textColorWhite medium-11">' +
                'Pour enrgistrez vos playlist et faire connaitre à Claude vos artistes et vos lieux favoris ' +
                '</b>' +
                '</p>')
       });
       updateInfo();
   }).error(function (data, status, headers, config) {
   });
   var i = -1 ;
    var changeInf;
    function removeAnimation (i) {
        $scope.infos[i].animation = '';
        $scope.elementEnCours.animation = '';
    }
   function updateInfo(){
       if(i === $scope.infos.length - 1){
           i = 0;
       } else {
           i++;
       }
       $scope.elementEnCours = {};
       $timeout(function () {
           $scope.$apply(function () {
               $scope.elementEnCours = $scope.infos[i];
           })
       }, 0);
       $scope.removeAnimation = function () {
           removeAnimation(i)
       };
       changeInf = $timeout(updateInfo,8000);
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