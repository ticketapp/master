angular.module('claudeApp').directive('ngSearchBar', function(){
    return{
        restrict : 'E',
        templateUrl:'assets/components/search/searchBar.html'
    }
});

angular.module('claudeApp').directive('ngSearch', function(){
    return{
        restrict : 'C',
        templateUrl:'assets/components/search/search.html',
        controller:'searchCtrl',
        link : function(scope, element){
            var positionElementInPage;
            var flag = false;
            if (window.location.hash.indexOf('search')>-1) {
                $(document).find('#searchTopBlock').removeClass("hiddenBlock");
                $(document).find('#searchTopBar').focus();
            } else {
                $(window).scroll(function() {
                    if (!flag){
                        positionElementInPage = $(element).find('#searchBar').offset().top;
                        flag = true;
                    }
                    if ($(window).scrollTop()>= positionElementInPage - 10) {
                        // fixed
                        $(element).find('#searchBlock').addClass("hiddenBlock");
                        $(document).find('#searchTopBlock').removeClass("hiddenBlock");
                        $(document).find('#searchTopBar').focus();
                    } else {
                        // relative
                        $(element).find('#searchBlock').removeClass("hiddenBlock");
                        $(element).find('#searchBlock').find('input').focus();
                        $(document).find('#searchTopBlock').addClass("hiddenBlock");
                        flag = false;
                    }
                });
            }
        }
    }
});
