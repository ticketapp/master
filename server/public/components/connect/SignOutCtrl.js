'use strict';


angular.module('claudeApp').controller('SignOutCtrl', ['$auth', function($auth) {
  console.log('lkjlkjlkj');
  if (!$auth.isAuthenticated()) {
    return;
  }
  $auth.logout()
    .then(function() {
      console.log("it worked, logged out :)");
      //$alert({
      //  content: 'You have been logged out',
      //  animation: 'fadeZoomFadeDown',
      //  type: 'material',
      //  duration: 3
      //});
    });
}]);
