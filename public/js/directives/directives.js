app.directive('ngEvent', function(){
    return{
      restrict : 'C',
      templateUrl:'assets/partials/_event.html'
    }
});
app.directive('ngEvents', function(){
    return{
      restrict : 'C',
      templateUrl:'assets/partials/events.html'
    }
});
app.directive('ngSearch', function(){
    return{
        restrict : 'C',
        templateUrl:'assets/partials/_search.html',
        controller:'searchCtrl',
        link : function(scope, element){
            var positionElementInPage;
            var flag = false;
            if ($(element).find('#searchBar').offset().top < 50) {
                // fixed
                $(element).find('#searchBlock').addClass("ng-hide");
                $(element).find('#events_contener').addClass("marginTop50 paddingTop10");
                $(document).find('#searchTopBlock').removeClass("ng-hide");
                $(document).find('#searchTopBar').focus();

            } else {
                // relative
                $(element).find('#searchBlock').removeClass("ng-hide");
                $(document).find('#searchTopBlock').addClass("ng-hide");
                flag = false;
            }
            $(window).scroll(function() {
                if (!flag){
                positionElementInPage = $(element).find('#searchBar').offset().top;
                flag = true;
                }
                if ($(window).scrollTop()>= positionElementInPage - 60) {
                    // fixed
                    $(element).find('#searchBlock').addClass("ng-hide");
                    $(document).find('#searchTopBlock').removeClass("ng-hide");
                    $(document).find('#searchTopBar').focus();
                    $(element).find('#events_contener').addClass("marginTop50 paddingTop10");

                } else {
                    // relative
                    $(element).find('#searchBlock').removeClass("ng-hide");
                    $(document).find('#searchTopBlock').addClass("ng-hide");
                    flag = false;
                }
            });
        }
    }
});

app.directive('ngVideo', function(){
    return{
      restrict : 'C',
      templateUrl:'assets/partials/_video.html'
    }
              
});

app.directive('ngMusique', function(){
    return{
      restrict : 'C',
      templateUrl:'assets/partials/_musique.html'
    }
});
app.directive('footer', function(){
    return{
      restrict : 'C',
      templateUrl:'assets/partials/_footer.html'
    }
});

app.directive('ngLecteur', function(){
    return{
        restrict : 'C',
        link : function(scope, element){
            $(document).ready(function(){
                $('.ng-lecteur').mouseover(function(){
                var w = $(this).find('iframe').css('width');
                var h = $(this).find('iframe').css('height');
                $(this).find('.lecteur_controller').css({'width': w});
                $(this).find('.lecteur_controller').css({'height': h});
                $(this).find('.lecteur_controller').css({'display': 'block'});
                });
                $('.ng-lecteur').mouseleave(function(){
                $(this).find('.lecteur_controller').css({'display': 'none'});
                });
                $(element).find('.lecteur_controller').find('a').click(function(){
                    $('.lecteur_content').find('iframe').remove();
                    $(this).parent().parent().find('iframe').clone().appendTo(".lecteur_content");
                    var bl = $('ng-lecture').css('bottom'); 
                    if(bl !=0){ $('.ng-lecture').animate({'bottom': 0}, 2000);
                              $('.restaurer').remove();
                              $('.reduire').remove();
                              $('.ng-control').append('<a class="reduire"> - </a>');
                    }
                    $('.ng-control').css({'display':'block'});
                    
                });
        });
    }
    }
});

app.directive('ngLecture', function(){
    return{
        restrict : 'C',
        templateUrl : 'assets/partials/_lecteur.html'
    }
});

app.directive('ngControl', function(){
    return{
        restrict : 'C',
        link : function(scope, element){
            $(element).find('.close').click(function(){
                $(this).parent().parent().find('iframe').remove();
                $(element).css({'display':'none'})
            });
            $(element).mouseenter(function(){
            $(element).find('.reduire').click(function(){
                var hr = $('.lecteur_content').css('height');
                $(element).parent().animate({'bottom':'-'+hr},2000);
                $('.reduire').remove();
                $('.restaurer').remove();
                $('.ng-control').append('<a class="restaurer">+</a>');
                $(element).find('.restaurer').click(function(){
                    $(element).parent().animate({'bottom': 0}, 1500);
                    $('.restaurer').remove();
                    $('.reduire').remove();
                    $('.ng-control').append('<a class="reduire">-</a>');
                 });
            });

            });
        }
    }
});


app.directive('customTrigger', function () {
    return {
        restrict: 'C',
        link: function (scope, element, attrs) {
             $(element).on('mouseover', function () {
                 var done = false;
                setTimeout(function () {
                    if ($('.tooltip').mouseover) {
                        $('.tooltip').mouseleave(function () {
                            scope.tt_isOpen = false;
                            $(this).remove();
                        });
                    }
                    setTimeout(function(){
                        $('.wrapper-outer').mouseover(function(){
                            if (!done)
                            {
                                scope.tt_isOpen = false;
                                $('.tooltip').remove();
                                done = true;
                            }
                        });
                    }, 500);
                }, 200);
            });
        }
    }
});

app.directive("fileread", [function () {
    return {
        scope: {
            fileread: "="
        },
        link: function (scope, element, attributes) {
            element.bind("change", function (changeEvent) {
                var reader = new FileReader();
                reader.onload = function (loadEvent) {
                    scope.$apply(function () {
                        scope.fileread = loadEvent.target.result;
                    });
                }
                reader.readAsDataURL(changeEvent.target.files[0]);
            });
        }
    }
}]);