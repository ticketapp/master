app.directive('wysiwyg',function(){
    return {
        restrict:'E',
        require: '?ngModel',
        controller : 'wysiwygCtrl',
        link : function(attr, element){
            element.find('iframe').find('head').append('<link rel="stylesheet" href="styles/main.css">');
        }
    }

});

app.directive('Wysiwygcontent', ['$sce', function($sce) {
    return {
        restrict: 'A', // only activate on element attribute
        require: '?ngModel', // get a hold of NgModelController
        link: function(scope, element, attrs, ngModel) {
            if (!ngModel) return; // do nothing if no ng-model

            // Specify how UI should be updated
            ngModel.$render = function() {
                element.html($sce.getTrustedHtml(ngModel.$viewValue || ''));
            };

            // Listen for change events to enable binding
            element.on('blur keyup change', function() {
                scope.$evalAsync(read);
            });
            read(); // initialize

            // Write data to the model
            function read() {
                var html = element.html();
                // When we clear the content editable the browser leaves a <br> behind
                // If strip-br attribute is provided then we strip this out
                if ( attrs.stripBr && html == '<br>' ) {
                    html = '';
                }
                ngModel.$setViewValue(html);
            }
        }
    };
}]);

app.controller('wysiwygCtrl', function($scope, $timeout){
    $scope.styles = [
        {
            'type':'button',
            'name':'bold',
            'function':'format("bold")'
        },
        {
            'type':'button',
            'name':'italic',
            'function':'format("italic")'
        },
        {
            'type':'button',
            'name':'Underline',
            'function':'format("Underline")'
        },
        {
            'type':'button',
            'name':'StrikeThrough',
            'function':'format("StrikeThrough")'
        },
        {
            'type':'button',
            'name':'JustifyLeft',
            'function':'format("JustifyLeft")'
        },
        {
            'type':'button',
            'name':'JustifyRight',
            'function':'format("JustifyRight")'
        },
        {
            'type':'button',
            'name':'JustifyCenter',
            'function':'format("JustifyCenter")'
        },
        {
            'type':'button',
            'name':'JustifyFull',
            'function':'format("JustifyFull")'
        },
        {
            'type':'button',
            'name':'InsertUnOrderedList',
            'function':'format("InsertUnOrderedList")'
        },
        {
            'type':'button',
            'name':'InsertOrderedList',
            'function':'format("InsertOrderedList")'
        },
        {
            'type':'button',
            'name':'ActiveElement',
            'function':'ActiveElement()'
        },
        {
            'type': 'select',
            'name': 'Font'
        },
        {
            'type': 'option',
            'typeName':'font',
            'name': 'Arial',
            'function': 'format("FontName", "arial")'
        },
        {
            'type': 'option',
            'typeName':'font',
            'name': 'Verdana',
            'function': 'format("FontName", "Verdana")'
        },
        {
            'type':'button',
            'name':'AddImage',
            'function':'addImage()'
        },
        {
            'type':'button',
            'name':'browsehappy',
            'function':'addClass("browsehappy")'
        },
        {
            'type':'select',
            'name':'ElementGrill'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "1")',
            'name': '1'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "2")',
            'name': '2'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "3")',
            'name': '3'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "4")',
            'name': '4'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "5")',
            'name': '5'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "6")',
            'name': '6'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "7")',
            'name': '7'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "8")',
            'name': '8'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "9")',
            'name': '9'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "10")',
            'name': '10'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "11")',
            'name': '11'
        },
        {'type': 'option',
            'typeName':'ElementGrill',
            'function':'ElementSize("large-", "12")',
            'name': '12'
        },
        {
            'type' : 'select',
            'name' : 'float'
        },
        {
            'type':'option',
            'typeName': 'float',
            'function' : 'ElementFloat("float-", "left")',
            'name' : 'Float Left'
        },
        {
            'type':'option',
            'typeName': 'float',
            'function' : 'ElementFloat("float-", "right")',
            'name' : 'Float Right'
        },
        {
            'type':'option',
            'typeName': 'float',
            'function' : 'ElementFloat("float-", "none")',
            'name' : 'Float None'
        },
        {
            'type': 'select',
            'name': 'Size'
        },
        {'type': 'option',
         'typeName':'Size',
         'value': '1',
         'function':'format("FontSize", 1)',
         'name': '1 (petite)'
        },
        {'type': 'option',
         'typeName':'Size',
        'function':'format("FontSize", "2")',
        'value': '2',
         'name': '2'
        },
        {'type': 'option',
            'typeName':'Size',
            'function':'format("FontSize", "3")',
            'value': '3',
            'name': '3 (normale)'
        },
        {'type': 'option',
            'typeName':'Size',
            'function':'format("FontSize", "4")',
            'value': '4',
            'name': '4'
        },
        {'type': 'option',
            'typeName':'Size',
            'function':'format("FontSize", "5")',
            'value': '5',
            'name': '5'
        },
        {'type': 'option',
            'typeName':'Size',
            'function':'format("FontSize", "6")',
            'value': '6',
            'name': '6'
        },
        {'type': 'option',
            'typeName':'Size',
            'function':'format("FontSize", "7")',
            'value': '7',
            'name': '7 (grande)'
        },
        {
            'type':'button',
            'name':'respContent',
            'function':'respContent()'
        }

    ];
    $scope.template = [{'name': '1', 'code': '<div class="row"><div class="large-12 columns"><img class="width100p" src="http://placehold.it/1000x400&text=[img]"><hr></div></div><div class="row"><div class="large-4 columns"><img src="http://placehold.it/400x300&text=[img]"></div><div class="large-8 columns"><h4>This is a content section.</h4><div class="row"><div class="large-6 columns"><p>Bacon ipsum dolor sit amet nulla ham qui sint exercitation eiusmod commodo, chuck duis velit. Aute in reprehenderit, dolore aliqua non est magna in labore pig pork biltong. Eiusmod swine spare ribs reprehenderit culpa. Boudin aliqua adipisicing rump corned beef.</p></div><div class="large-6 columns"><p>Pork drumstick turkey fugiat. Tri-tip elit turducken pork chop in. Swine short ribs meatball irure bacon nulla pork belly cupidatat meatloaf cow. Nulla corned beef sunt ball tip, qui bresaola enim jowl. Capicola short ribs minim salami nulla nostrud pastrami.</p></div></div></div></div><div class="row"><div class="large-8 columns"><h4>This is a content section.</h4><p>Bacon ipsum dolor sit amet nulla ham qui sint exercitation eiusmod commodo, chuck duis velit. Aute in reprehenderit, dolore aliqua non est magna in labore pig pork biltong. Eiusmod swine spare ribs reprehenderit culpa. Boudin aliqua adipisicing rump corned beef.</p><p>Pork drumstick turkey fugiat. Tri-tip elit turducken pork chop in. Swine short ribs meatball irure bacon nulla pork belly cupidatat meatloaf cow. Nulla corned beef sunt ball tip, qui bresaola enim jowl. Capicola short ribs minim salami nulla nostrud pastrami.</p></div><div class="large-4 columns"><img src="http://placehold.it/400x300&text=[img]"></div></div>'}, {'name': '2', 'code': '<div class="row"><div class="large-3 columns"><h1><img src="http://placehold.it/400x100&text=Logo"></h1></div><div class="large-9 columns"><ul class="button-group right"><li><a href="#" class="button">another temps</a></li><li><a href="#" class="button">Link 2</a></li><li><a href="#" class="button">Link 3</a></li><li><a href="#" class="button">Link 4</a></li></ul></div></div><div class="row"><div class="large-12 columns"><img src="http://placehold.it/1000x400&text=[img]"><hr></div></div><div class="row"><div class="large-4 columns"><img src="http://placehold.it/400x300&text=[img]"></div><div class="large-8 columns"><h4>This is a content section.</h4><div class="row"><div class="large-6 columns"><p>Bacon ipsum dolor sit amet nulla ham qui sint exercitation eiusmod commodo, chuck duis velit. Aute in reprehenderit, dolore aliqua non est magna in labore pig pork biltong. Eiusmod swine spare ribs reprehenderit culpa. Boudin aliqua adipisicing rump corned beef.</p></div><div class="large-6 columns"><p>Pork drumstick turkey fugiat. Tri-tip elit turducken pork chop in. Swine short ribs meatball irure bacon nulla pork belly cupidatat meatloaf cow. Nulla corned beef sunt ball tip, qui bresaola enim jowl. Capicola short ribs minim salami nulla nostrud pastrami.</p></div></div></div></div><div class="row"><div class="large-8 columns"><h4>This is a content section.</h4><p>Bacon ipsum dolor sit amet nulla ham qui sint exercitation eiusmod commodo, chuck duis velit. Aute in reprehenderit, dolore aliqua non est magna in labore pig pork biltong. Eiusmod swine spare ribs reprehenderit culpa. Boudin aliqua adipisicing rump corned beef.</p><p>Pork drumstick turkey fugiat. Tri-tip elit turducken pork chop in. Swine short ribs meatball irure bacon nulla pork belly cupidatat meatloaf cow. Nulla corned beef sunt ball tip, qui bresaola enim jowl. Capicola short ribs minim salami nulla nostrud pastrami.</p></div><div class="large-4 columns"><img src="http://placehold.it/400x300&text=[img]"></div></div>'}];
    $scope.useTemps = function (name) {
        for (var i=0; i< $scope.template.length; i++) {
            console.log(name);
            if (name == $scope.template[i].name) {
                document.getElementById("content").innerHTML = $scope.template[i].code;
            }
        }
    };
    $scope.respContent = function(){
        $scope.content = document.getElementById("content").innerHTML;
        var respContent = document.getElementById("content").outerHTML;
        var iframe = document.getElementsByTagName('iframe')[0].contentDocument.getElementsByTagName('body')[0].innerHTML;
        document.getElementsByTagName('iframe')[0].contentDocument.getElementsByTagName('body')[0].children['content'].outerHTML = respContent;
        console.log(document.getElementsByTagName('iframe')[0].contentDocument.getElementsByTagName('body')[0].children);
    };

    $scope.format = function (a, b) {
        if (window.getSelection().toString().length > 0) {
            document.execCommand(a, false, b);
        }
    };
    $scope.addClass = function(a){
        if (document.getSelection().anchorNode.parentNode.className == a){
            var content = document.getSelection().anchorNode.parentNode.classList;
            content.remove(a);
        } else
        {
            document.execCommand("insertHTML", false, "<span class='" + a + "'>" + document.getSelection() + "</span>");
        }
    };
    $scope.ElementSize = function(a, b) {
        range = window.getSelection().getRangeAt(0);
        if (range == range.commonAncestorContainer.data){
            if (range.commonAncestorContainer.parentElement.localName != "div") {
                var flag = true;
                var rangeSave = range.commonAncestorContainer.parentElement;
                while (flag == true){
                    if (rangeSave.outerHTML == rangeSave.parentElement.innerHTML) {
                        if (rangeSave.parentElement.localName == "div") {
                            flag = false;
                            range = rangeSave;
                            if (range.parentElement.id == "content") {
                                range = range.outerHTML;
                                createNewColumn();
                            } else {
                                range = range.parentElement;
                                changeColumnClass();
                            }
                        } else {
                            rangeSave = rangeSave.parentElement;
                        }
                    } else {
                        flag = false;
                        range = rangeSave.outerHTML;
                        createNewColumn();
                    }
                }
            } else {
                if (range.commonAncestorContainer.parentElement.id == "content") {
                    var div = document.createElement("div");
                    div.innerHTML = "<div  class='column " + a + b + "'>" + range + "</div>";
                    range.deleteContents();
                    var frag = document.createDocumentFragment(), child;
                    while ( (child = div.firstChild) ) {
                        frag.appendChild(child);
                    }
                    range.insertNode(frag);
                } else {
                    range = range.commonAncestorContainer.parentElement;
                    changeColumnClass();
                }
            }
        } else {
            var div = document.createElement("div");
            div.innerHTML = "<div  class='column " + a + b + "'>" + range + "</div>";
            range.deleteContents();
            var frag = document.createDocumentFragment(), child;
            while ( (child = div.firstChild) ) {
                frag.appendChild(child);
            }
            range.insertNode(frag);
        }
        function changeColumnClass () {
            var gridClass = range.className;
            var css = a + b;
            var del = gridClass.indexOf(css);
            var change = gridClass.indexOf(a);
            if ( del > 0) {
                range.classList.remove(css);
                range.classList.remove("column");
            } else if (change > 0){
               // var pattern = new RegExp(a, /\d/);
                var pattern = new RegExp(a + "[0-9][0-9]?");
                if (matched = gridClass.match(pattern)) {
                    range.classList.remove(matched[0]);
                    range.classList.add(css);
                }
            } else {
                range.classList.add(css);
            }
        }
        function createNewColumn () {
            console.log(range);
            document.execCommand("insertHTML", false, "<div  class='column " + a + b + "'>" + range + "</div>");
        }
    };
    $scope.ElementFloat = function(a, b){
        range = window.getSelection().getRangeAt(0);
        if (range == range.commonAncestorContainer.data){
            if (range.commonAncestorContainer.parentElement.localName != "div") {
                var flag = true;
                var rangeSave = range.commonAncestorContainer.parentElement;
                while (flag == true){
                    if (rangeSave.outerHTML == rangeSave.parentElement.innerHTML) {
                        if (rangeSave.parentElement.localName == "div") {
                            flag = false;
                            range = rangeSave;
                            if (range.parentElement.id == "content") {
                                range = range.outerHTML;
                                createNewFloat();
                            } else {
                                range = range.parentElement;
                                changeFloatClass();
                            }
                        } else {
                            rangeSave = rangeSave.parentElement;
                        }
                    } else {
                        flag = false;
                        range = rangeSave.outerHTML;
                        createFloatColumn();
                    }
                }
            } else {
                if (range.commonAncestorContainer.parentElement.id == "content") {
                    var div = document.createElement("div");
                    div.innerHTML = "<div  class='" + a + b + "'>" + range + "</div>";
                    range.deleteContents();
                    var frag = document.createDocumentFragment(), child;
                    while ( (child = div.firstChild) ) {
                        frag.appendChild(child);
                    }
                    range.insertNode(frag);
                } else {
                    range = range.commonAncestorContainer.parentElement;
                    changeFloatClass();
                }
            }
        } else {
            var div = document.createElement("div");
            div.innerHTML = "<div  class='" + a + b + "'>" + range + "</div>";
            range.deleteContents();
            var frag = document.createDocumentFragment(), child;
            while ( (child = div.firstChild) ) {
                frag.appendChild(child);
            }
            range.insertNode(frag);
        }
        function changeFloatClass () {
            var gridClass = range.className;
            var css = a + b;
            var del = gridClass.indexOf(css);
            console.log(del);
            var change = gridClass.indexOf(a);
            if ( del >= 0) {
                range.classList.remove(css);
            } else if (change >= 0) {
                var pattern = new RegExp(a + "\\w*");
                console.log(a);
                console.log(gridClass);
                console.log(gridClass.match(pattern));
                if (matched = gridClass.match(pattern)) {
                    range.classList.remove(matched[0]);
                    range.classList.add(css);
                }
            } else {
                range.classList.add(css);
            }
        }
        function createNewFloat () {
            document.execCommand("insertHTML", false, "<div  class='" + a + "'>" + range + "</div>");
        }
    };
    $scope.addImage = function(img){
        document.execCommand("InsertImage", false, img);
        var last = document.getElementById("content").getElementsByTagName('img').length;
        console.log(document.getElementById("content").getElementsByTagName('img')[last-1]);
        document.getElementById("content").getElementsByTagName('img')[last-1].classList.add("width100p");
    };



    function ActiveElement (event){
        var sidePoint = event.clientX;
        var sidePointY = event.clientY;
        var toRemoveCss = document.getElementById("content").getElementsByTagName('div');
        var toRemoveSpanCss = document.getElementById("content").getElementsByTagName('span');
        var searchM;
        var imgFind = false;
        var imgs = document.getElementById("content").getElementsByTagName('img');
        for (i = 0; i < toRemoveCss.length; i++) {
            toRemoveCss[i].style.boxShadow = "";

        }
        for (i = 0; i < imgs.length; i++) {
            imgs[i].style.boxShadow = "";
        }
        for (i = 0; i < toRemoveSpanCss.length; i++) {
            toRemoveSpanCss[i].style.backgroundColor = "";

        }
        for (var ii = 0; ii < imgs.length; ii++) {
            var posEl = imgs[ii].getBoundingClientRect();
            if(sidePoint > posEl.left && sidePoint < posEl.right && sidePointY < posEl.bottom && sidePointY > posEl.top) {
                imgFind = true;
                searchM = document.getElementById("content").getElementsByTagName('img')[ii];
            }
        }
       $timeout(function () {
           var searchParent = true;
           var m = false;
           //var m = window.getSelection().anchorNode.parentElement;
           if (imgFind == true) {
               m = searchM;
               searchParent = false;
           } else {
               searchM = window.getSelection().anchorNode.parentElement;
           }
           while (searchParent == true) {
               switch (searchM.tagName) {
                   case 'DIV':
                   case 'WYSIWYG':
                       m = searchM;
                       searchParent = false;
                       break;
                   case 'body':
                       m = document.getElementById("content");
                       searchParent = false;
                       break;
                   default:
                       searchM = searchM.parentElement;
                       break;
               }
           }
           var rect = m.getBoundingClientRect();
           if (sidePoint > rect.right || sidePoint < rect.left || sidePointY < rect.top || sidePointY > rect.bottom ) {
               m = m.parentElement;
               rect = m.getBoundingClientRect();
               console.log(m);
           }

           $scope.background = function (bc) {
               console.log(bc);
               m.style.backgroundColor = bc;
           };
           if (m.id == "content") {
               var inner = m.innerHTML;
               m.innerHTML = inner + "<div  class='column' contenteditable='true'>MyNewDiv</div>";
           } else if (m.tagName == "WYSIWYG" || m.tagName == "BODY") {
               var contentElement = document.getElementById('content').innerHTML;
               document.getElementById('content').innerHTML = contentElement + "<div contenteditable='true' class='column'>MyNewDiv</div>";
           } else {
               m.contentEditable = true;
               m.style.boxShadow = "0 0 0 1px rgb(0, 140, 186)";
               var onResize = false;
               var right = false;
               var left = false;
               var parentRect = m.parentElement.getBoundingClientRect();
               var offset = parentRect.width * 8 / 100;
               if (sidePoint > rect.left && sidePoint < rect.left + 5) {
                   if (right == false) {
                       onResize = true;
                       m.style.cursor = "col-resize";
                       left = true;
                   }
               } else if (sidePoint < rect.right && sidePoint > rect.right - 5) {
                   if (left == false) {
                       onResize = true;
                       m.style.cursor = "col-resize";
                       right = true;
                   }
               } else {
                   right = false;
                   left = false;
                   onResize = false;
               }
               m.addEventListener('mouseup', function () {
                   onResize = false;
                   right = false;
                   left = false;
                   m.style.cursor = "auto";
               });
               m.parentElement.addEventListener('mouseup', function () {
                   onResize = false;
                   m.style.cursor = "auto";
                   right = false;
                   left = false;
               });
               m.parentElement.addEventListener("mousemove", function (event) {
                   var cursorPosition = event.clientX;
                   if (event.clientX > rect.left && event.clientX < rect.left + 5) {
                       m.style.cursor = "col-resize";
                   } else if (event.clientX < rect.right && event.clientX > rect.right - 5) {
                       m.style.cursor = "col-resize";
                   } else if (onResize == false) {
                       m.style.cursor = "auto";
                   }
                   if (onResize == true) {
                       if (left == true && cursorPosition > sidePoint + offset) {
                           sidePoint = sidePoint + offset;
                           var typeClass = "large-offset-";
                           var gridClass = m.className;
                           var pattern = new RegExp(typeClass + "\\w*");
                           if (matched = gridClass.match(pattern)) {
                               if (matched[0] != "large-offset-11") {
                                   var largeClass = "large-";
                                   var large = new RegExp(largeClass + "[0-9][0-9]?");
                                   var number = Number(matched[0].match(/\d+/)) + 1;
                                   if (matchedLarge = gridClass.match(large)) {
                                       var numberLargeClass = Number(matchedLarge[0].match(/\d+/)) - 1;
                                       var maxLarge = number + numberLargeClass;
                                       if (maxLarge >= 12) {
                                           m.classList.remove(matchedLarge[0]);
                                           m.classList.add("large-" + numberLargeClass);
                                       }
                                   }
                                   var css = "large-offset-" + number;
                                   m.classList.remove(matched[0]);
                                   m.classList.add(css);
                               }
                           } else {
                               m.classList.add('large-offset-1');
                               //rect = m.getBoundingClientRect();
                           }
                       }
                       if (left == true && cursorPosition < sidePoint - offset) {
                           sidePoint = sidePoint - offset;
                           console.log('remove offset');
                           typeClass = "large-offset-";
                           gridClass = m.className;
                           pattern = new RegExp(typeClass + "\\w*");
                           if (matched = gridClass.match(pattern)) {
                               if (matched[0] != "large-offset-1") {
                                   number = Number(matched[0].match(/\d+/)) - 1;
                                   css = "large-offset-" + number;
                                   m.classList.remove(matched[0]);
                                   m.classList.add(css);
                               } else {
                                   m.classList.remove('large-offset-1');
                               }
                           } else {
                               m.classList.add('float-left');
                               console.log('here')
                           }
                       }
                       if (right == true && cursorPosition > sidePoint + offset) {
                           sidePoint = sidePoint + offset;
                           console.log('addlarge');
                           var LargeClass = "large-";
                           var gridClass = m.className;
                           var patternLarge = new RegExp(LargeClass + "[0-9][0-9]?");
                           if (matched = gridClass.match(patternLarge)) {
                               var offsetClass = "large-offset-";
                               var offsetClassPattern = new RegExp(offsetClass + "\\w*");
                               if (matchedOffset = gridClass.match(offsetClassPattern)) {
                                   var numberOffset = Number(matchedOffset[0].match(/\d+/));
                                   var maxLargeClass = 12 - numberOffset;
                                   m.classList.remove(matched[0]);
                                   m.classList.add("large-" + maxLargeClass);
                               } else if (matched[0] != "large-12") {
                                   var numberLarge = Number(matched[0].match(/\d+/)) + 1;
                                   var cssLarge = "large-" + numberLarge;
                                   m.classList.remove(matched[0]);
                                   m.classList.add(cssLarge);
                               }
                           }
                       }
                       if (right == true && cursorPosition < sidePoint - offset) {
                           sidePoint = sidePoint - offset;
                           console.log('remove large');
                           LargeClass = "large-";
                           gridClass = m.className;
                           patternLarge = new RegExp(LargeClass + "[0-9][0-9]?");
                           if (matchedLarge = gridClass.match(patternLarge)) {
                               numberLarge = Number(matchedLarge[0].match(/\d+/)) - 1;
                               if (numberLarge > 0) {
                                   css = "large-" + numberLarge;
                                   console.log(css + "remove");
                                   m.classList.remove(matchedLarge[0]);
                                   m.classList.add(css);
                               }
                           } else {
                               m.classList.add('column');
                               var offsetClass = "large-offset-";
                               var offsetClassPattern = new RegExp(offsetClass + "\\w*");
                               if (matchedOffset = gridClass.match(offsetClassPattern)) {
                                   var numberOffset = Number(matchedOffset[0].match(/\d+/));
                                   var maxLargeClass = 12 - numberOffset;
                                   m.classList.add("large-" + maxLargeClass);
                               } else {
                                   m.classList.add('large-12');
                               }
                           }
                       }
                   }
               });
           }
       }, 100);
    }
    document.getElementById("content").addEventListener("mousedown", ActiveElement, false);


});