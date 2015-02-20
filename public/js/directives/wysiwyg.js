app.directive('wysiwyg',function(){
    return {
        restrict:'E',
        require: '?ngModel',
        controller : 'wysiwygCtrl',
        link : function(attr, element){
            //element.find('iframe').find('head').append('<link rel="stylesheet" href="styles/main.css">');
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

app.controller('wysiwygCtrl', function($scope, $timeout, $location){
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
    $scope.maxlargeClass = 12;
    $scope.$watch("newSize", function(newval) {
        if (newval != undefined) {
            var val = newval.toString()
            val = val.replace('.0', '');
            if (val <= 12 && val >= 1) {
                $scope.ElementSize('large-', val);
            }
        }
    }, true);
    $scope.$watch("newOffset", function(newval) {
        if (newval != undefined) {
            var val = newval.toString()
            val = val.replace('.0', '');
            if (val <= 12 && val >= 1) {
                $scope.ElementSize('large-offset-', val - 1);
            }
        }
    }, true);
    var small;
    var medium;
    var large;
    var respClass;
    $scope.maxWinWidth = function () {
        $scope.winWidth = window.innerWidth;
        if (window.innerWidth > 950) {
            large = true;
            medium = false;
            small = false;
            respClass = "large-";
        } else if (window.innerWidth < 950 && window.innerWidth > 350){
            medium = true;
            large = false;
            small = false;
            respClass = "medium-";
        } else if (window.innerWidth <= 350){
            small = true;
            large = false;
            medium = false;
            respClass = "small-";
        }
        $scope.showRemEl = false;
    };
    $scope.maxWinWidth();
    $scope.winHeight = window.innerHeight - 49 + "px";
    function wysinit () {
        //var document = document.getElementsByTagName('iframe')[0].contentDocument;
        //console.log(document)
        $scope.template = [
            {'name': '1', 'code': '<div class="row"><div class="large-12 columns"><img class="width100p" src="http://placehold.it/1000x400&text=[img]"><hr></div></div><div class="row"><div class="large-4 columns"><img src="http://placehold.it/400x300&text=[img]"></div><div class="large-8 columns"><h4>This is a content section.</h4><div class="row"><div class="large-6 columns"><p>Bacon ipsum dolor sit amet nulla ham qui sint exercitation eiusmod commodo, chuck duis velit. Aute in reprehenderit, dolore aliqua non est magna in labore pig pork biltong. Eiusmod swine spare ribs reprehenderit culpa. Boudin aliqua adipisicing rump corned beef.</p></div><div class="large-6 columns"><p>Pork drumstick turkey fugiat. Tri-tip elit turducken pork chop in. Swine short ribs meatball irure bacon nulla pork belly cupidatat meatloaf cow. Nulla corned beef sunt ball tip, qui bresaola enim jowl. Capicola short ribs minim salami nulla nostrud pastrami.</p></div></div></div></div><div class="row"><div class="large-8 columns"><h4>This is a content section.</h4><p>Bacon ipsum dolor sit amet nulla ham qui sint exercitation eiusmod commodo, chuck duis velit. Aute in reprehenderit, dolore aliqua non est magna in labore pig pork biltong. Eiusmod swine spare ribs reprehenderit culpa. Boudin aliqua adipisicing rump corned beef.</p><p>Pork drumstick turkey fugiat. Tri-tip elit turducken pork chop in. Swine short ribs meatball irure bacon nulla pork belly cupidatat meatloaf cow. Nulla corned beef sunt ball tip, qui bresaola enim jowl. Capicola short ribs minim salami nulla nostrud pastrami.</p></div><div class="large-4 columns"><img src="http://placehold.it/400x300&text=[img]"></div></div>'},
            {'name': '2', 'code': '<div class="parallax-background" contenteditable="true">  <div class="intro-text" contenteditable="true" >    scroll for parallax!    <p><i class="fi-arrow-down"></i></p>  </div></div><div class="parallax-content" contenteditable="true">  <div contenteditable="true"><p>Bacon ipsum dolor sit amet chicken pork loin hamburger ball tip, swine tenderloin salami meatball boudin tri-tip fatback shank bacon. Pork salami kevin, brisket spare ribs strip steak ham hock porchetta. Pork belly short loin tail porchetta. Pork pig meatball, beef ribs doner jerky tri-tip hamburger filet mignon strip steak ball tip ham tail tongue corned beef.</p></div>  <div contenteditable="true"><p>Flank spare ribs capicola, strip steak biltong pancetta bresaola tri-tip cow landjaeger. Short ribs sirloin beef ribs, flank capicola ribeye turducken. Sirloin boudin andouille tail. Ham flank tail sausage t-bone, jerky landjaeger kevin porchetta ground round pork belly.</p></div>  <div contenteditable="true"><p>Pork loin doner biltong shoulder meatball flank. Sirloin shankle ground round tail, short loin prosciutto beef ribs salami pork pancetta kielbasa. Brisket spare ribs jerky filet mignon flank. Turducken ham sausage pork loin salami. Ball tip meatloaf shank pancetta hamburger meatball. Jowl beef pork loin tenderloin turducken, pastrami capicola filet mignon ham hock pork belly t-bone jerky ball tip kevin.</p></div>  <div contenteditable="true"><p>Filet mignon shank pastrami pork loin ham jerky corned beef chuck drumstick ribeye prosciutto pork belly tail. Beef leberkas brisket ham hock tenderloin tail tri-tip pork chop. Corned beef boudin andouille tail fatback chicken strip steak biltong pancetta pork loin tri-tip landjaeger pastrami kevin. Tongue spare ribs kevin ribeye shoulder swine.</p></div>  <div contenteditable="true"><p>Chicken kielbasa swine ham hock filet mignon strip steak bacon fatback pork t-bone. Ground round shankle tongue, jowl flank spare ribs beef leberkas ham boudin shank capicola short ribs. Drumstick pork loin turkey pork chop salami andouille pork belly. Short loin tongue tri-tip, hamburger filet mignon pastrami doner shoulder turkey ball tip t-bone jerky ham chicken.</p></div>  <div contenteditable="true"><p>Ground round flank pork loin salami, cow biltong hamburger swine brisket tongue tenderloin drumstick meatball. Biltong tongue salami ham shankle, leberkas meatloaf sirloin ball tip. Pork loin pastrami spare ribs beef capicola sausage kevin filet mignon pork chop. Bacon jerky turkey shoulder cow capicola ball tip shank. Ham tongue tail tri-tip kielbasa ham hock frankfurter meatball cow brisket. Shankle sausage filet mignon biltong pork loin. Short ribs spare ribs chuck pig meatloaf shoulder cow tail brisket biltong ball tip ham hock.</p></div></div>'}
        ];
        $scope.useTemps = function (name) {
            for (var i = 0; i < $scope.template.length; i++) {
                console.log(name);
                if (name == $scope.template[i].name) {
                    document.getElementById("content").innerHTML = $scope.template[i].code;
                }
            }
        };
        $scope.respContent = function () {
            $scope.winWidth = '350px';
            large = false;
            medium = false;
            small = true;
            respClass = "small-";
            $scope.showRemEl = false;
        };

        $scope.format = function (a, b) {
            if (document.getSelection().toString().length > 0) {
                document.execCommand(a, false, b);
            }
        };
        $scope.addClass = function (a) {
            if (document.getSelection().anchorNode.parentNode.className == a) {
                var content = document.getSelection().anchorNode.parentNode.classList;
                content.remove(a);
            } else {
                document.execCommand("insertHTML", false, "<span class='" + a + "'>" + document.getSelection() + "</span>");
            }
        };
        $scope.addImage = function (img) {
            document.execCommand("InsertImage", false, img);
            var last = document.getElementById("content").getElementsByTagName('img').length;
            console.log(document.getElementById("content").getElementsByTagName('img')[last - 1]);
            document.getElementById("content").getElementsByTagName('img')[last - 1].classList.add("width100p");
        };
        function ActiveElement(event) {
            $scope.showRemEl = false;
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
                if (sidePoint > posEl.left && sidePoint < posEl.right && sidePointY < posEl.bottom && sidePointY > posEl.top) {
                    imgFind = true;
                    if (document.getElementById("content").getElementsByTagName('img')[ii].parentNode.className.indexOf("imgContain") > -1) {
                        searchM = document.getElementById("content").getElementsByTagName('img')[ii].parentNode;
                    } else {
                        document.getElementById("content").getElementsByTagName('img')[ii].outerHTML = '<div class="imgContain padding0">' + document.getElementById("content").getElementsByTagName('img')[ii].outerHTML + '</div>'
                        if (document.getElementById("content").getElementsByTagName('img')[ii].className.indexOf("width100p") <= -1) {
                            document.getElementById("content").getElementsByTagName('img')[ii].classList.add("width100p");
                        }
                        searchM = document.getElementById("content").getElementsByTagName('img')[ii].parentNode
                    }
                }
            }
            $timeout(function () {
                var searchParent = true;
                var m = false;
                if (imgFind == true) {
                    m = searchM;
                    searchParent = false;
                } else {
                    if (document.getSelection().anchorNode != null  && document.getSelection().anchorNode.parentElement.tagName != 'A'){
                        searchM = document.getSelection().anchorNode.parentElement;
                    } else {
                        searchM = document.getElementById('content');
                    console.log(searchM)
                    }
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
                function initControl () {
                var posEl = m.getBoundingClientRect();
                document.getElementById('remEl').style.position = "fixed";
                document.getElementById('remEl').style.zIndex = 10;
                document.getElementById('remEl').style.top = posEl.top - 20 + "px";
                document.getElementById('remEl').style.left =  posEl.right - 10 + "px";
                /*document.getElementById('ElSize').style.position = "fixed";
                document.getElementById('ElSize').style.zIndex = 10;
                document.getElementById('ElSize').style.top =  posEl.top - 20 + "px";
                document.getElementById('ElSize').style.left = posEl.right - 70 + "px";*/
                $scope.showRemEl = true;
                }
                initControl();
                document.addEventListener('mouseover', initControl);
                window.addEventListener('scroll', initControl);
                $scope.$on('$locationChangeStart', function () {
                    window.removeEventListener('scroll', initControl);
                    window.removeEventListener('mouseover', initControl)
                });
                if (sidePoint > rect.right || sidePoint < rect.left || sidePointY < rect.top || sidePointY > rect.bottom) {
                    m = m.parentElement;
                    rect = m.getBoundingClientRect();
                    console.log(m);
                }

                $scope.background = function (bc) {
                    console.log(bc);
                    m.style.backgroundColor = bc;
                };
                $scope.ElementSize = function (a, b) {
                    $scope.sizeEl = b +"/12"
                    /*if (small == true) {
                        a = "small-"
                    } else if (medium == true) {
                        a = "medium-"
                    }else if (large == true) {
                        a = "large-"
                    }*/
                    if (imgFind == true) {
                        range = m;
                        changeColumnClass()
                    } else {
                        range = document.getSelection().getRangeAt(0);
                        if (range == range.commonAncestorContainer.data || range.endOffset == range.startOffset || range.commonAncestorContainer.parentElement.localName == 'a') {
                            if (range.commonAncestorContainer.parentElement.localName != "div") {
                                var flag = true;
                                var rangeSave = range.commonAncestorContainer.parentElement;
                                while (flag == true) {
                                    if (rangeSave.outerHTML == rangeSave.parentElement.innerHTML || range.commonAncestorContainer.parentElement.localName == 'a') {
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
                                /*if (range.commonAncestorContainer.parentElement.id == "content") {
                                    var div = document.createElement("div");
                                    div.innerHTML = "<div  class='column float-left " + a + b + "'>" + range + "</div>";
                                    range.deleteContents();
                                    var frag = document.createDocumentFragment(), child;
                                    while ((child = div.firstChild)) {
                                        frag.appendChild(child);
                                    }
                                    range.insertNode(frag);
                                } else {
                                    range = range.commonAncestorContainer.parentElement;
                                    changeColumnClass();
                                }*/
                                range = m;
                                changeColumnClass();
                            }
                        } else {
                            var div = document.createElement("div");
                            div.innerHTML = "<div  class='column float-left " + a + b + "'>" + range + "</div>";
                            range.deleteContents();
                            var frag = document.createDocumentFragment(), child;
                            while ((child = div.firstChild)) {
                                frag.appendChild(child);
                            }
                            range.insertNode(frag);
                        }
                    }
                    function changeColumnClass() {
                        var gridClass = range.className;
                        var css = a + b;
                        var del = gridClass.indexOf(css);
                        var change = gridClass.indexOf(a);
                        if (off = range.className.match(/offset-\d+/)) {
                            $scope.maxlargeClass = 12 -  parseInt(off[0].match(/\d+/))
                        } else {
                            $scope.maxlargeClass = 12
                        }
                        /*if (del > -1) {
                            range.classList.remove(css);
                            range.classList.remove("column");
                        }*/if (change > -1) {
                            // var pattern = new RegExp(a, /\d/);
                            if (a.indexOf('offset') > -1) {
                                if (sizeMatched = range.className.match(/large-\d+/)) {
                                    if(parseInt(sizeMatched[0].match(/\d+/))) {
                                        $scope.maxlargeClass = 12 - parseInt(b);
                                        if(parseInt(sizeMatched[0].match(/\d+/)) + parseInt(b) >= 12) {
                                            //range.classList.remove(sizeMatched[0]);
                                            var maxSize = 'large-' + (12 - parseInt(b)).toString();
                                            //range.classList.add(maxSize);
                                            $scope.newSize = (12 - parseInt(b)).toString() + '.0';
                                            console.log($scope.newSize)
                                        }
                                    }
                                }
                            }
                            var pattern = new RegExp(a + "[0-9][0-9]?");
                            if (matched = gridClass.match(pattern)) {
                                range.classList.remove(matched[0]);
                                range.classList.add(css);
                                if (gridClass.indexOf('float-left') <= -1) {
                                    range.classList.add('float-left');
                                }
                            }
                        } else {
                            range.classList.add(css);
                            if (a.indexOf('offset') > -1) {
                                if (sizeMatched = range.className.match(/large-\d+/)) {
                                    if(parseInt(sizeMatched[0].match(/\d+/))) {
                                        $scope.maxlargeClass = 12 - parseInt(b);
                                        console.log($scope.maxlargeClass)
                                        if(parseInt(sizeMatched[0].match(/\d+/)) + parseInt(b) >= 12) {
                                            //range.classList.remove(sizeMatched[0]);
                                            var maxSize = 'large-' + (12 - parseInt(b)).toString();
                                            //range.classList.add(maxSize);
                                            $scope.newSize = (12 - parseInt(b)).toString() + '.0';
                                        }
                                    }
                                }
                            }
                            if (gridClass.indexOf('float-left') <= -1) {
                                range.classList.add('float-left');
                            }
                        }
                    }

                    function createNewColumn() {
                        console.log(range);
                        document.execCommand("insertHTML", false, "<div  class='column float-left " + a + b + "'>" + range + "</div>");
                    }
                    initControl();
                };
                $scope.ElementFloat = function (a, b) {
                    if (imgFind == true) {
                        range = m;
                        changeFloatClass();
                    }
                    else {
                        range = document.getSelection().getRangeAt(0);
                        if (range == range.commonAncestorContainer.data || range.endOffset == range.startOffset) {
                            if (range.commonAncestorContainer.parentElement.localName != "div") {
                                var flag = true;
                                var rangeSave = range.commonAncestorContainer.parentElement;
                                while (flag == true) {
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
                                    while ((child = div.firstChild)) {
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
                            while ((child = div.firstChild)) {
                                frag.appendChild(child);
                            }
                            range.insertNode(frag);
                        }
                    }
                    function changeFloatClass() {
                        var gridClass = range.className;
                        var css = a + b;
                        var del = gridClass.indexOf(css);
                        console.log(del);
                        var change = gridClass.indexOf(a);
                        if (del >= 0) {
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
                        initControl();
                    }

                    function createNewFloat() {
                        document.execCommand("insertHTML", false, "<div  class='" + a + "'>" + range + "</div>");
                        initControl();
                    }
                };
                if (m.id == "content") {
                    //var inner = m.innerHTML;
                    //m.innerHTML = inner + "<div  class='column' contenteditable='true'>MyNewDiv</div>";
                } else if (m.tagName == "WYSIWYG" || m.tagName == "BODY") {
                    //var contentElement = document.getElementById('content').innerHTML;
                    //document.getElementById('content').innerHTML = contentElement + "<div contenteditable='true' class='column'>MyNewDiv</div>";
                } else if (document.getElementById('content').innerHTML.indexOf(m.outerHTML) == -1) {
                    //var contentElement = document.getElementById('content').innerHTML;
                    //document.getElementById('content').innerHTML = contentElement + "<div contenteditable='true' class='column'>MyNewDiv</div>";
                } else {
                    $scope.remEl = function () {
                        m.outerHTML = "";
                        $scope.showRemEl = false;
                    };
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
                        initControl();
                    });
                    m.parentElement.addEventListener('mouseup', function () {
                        onResize = false;
                        m.style.cursor = "auto";
                        right = false;
                        left = false;
                        initControl();
                    });
                    /*m.parentElement.addEventListener("mousemove", function (event) {
                        var cursorPosition = event.clientX;
                        if (event.clientX > rect.left && event.clientX < rect.left + 5) {
                            m.style.cursor = "col-resize";
                            if (imgFind == true) {
                                m.title = "Utilisez le bouton ElementSize pour redimentionner une image"
                            }
                        } else if (event.clientX < rect.right && event.clientX > rect.right - 5) {
                            m.style.cursor = "col-resize";
                            if (imgFind == true) {
                                m.title = "Utilisez le bouton ElementSize pour redimentionner une image"
                            }
                        } else if (onResize == false) {
                            m.style.cursor = "auto";
                            if (imgFind == true) {
                                m.title = ""
                            }
                        }
                        if (onResize == true) {
                            if (left == true && cursorPosition > sidePoint + offset) {
                                sidePoint = sidePoint + offset;
                                var typeClass = respClass + "offset-";
                                var gridClass = m.className;
                                var pattern = new RegExp(typeClass + "\\w*");
                                if (matched = gridClass.match(pattern)) {
                                    if (matched[0] != respClass + "offset-11") {
                                        var largeClass = respClass;
                                        var large = new RegExp(largeClass + "[0-9][0-9]?");
                                        var number = Number(matched[0].match(/\d+/)) + 1;
                                        if (matchedLarge = gridClass.match(large)) {
                                            var numberLargeClass = Number(matchedLarge[0].match(/\d+/)) - 1;
                                            var maxLarge = number + numberLargeClass;
                                            if (maxLarge >= 12) {
                                                var largeClassNumber = 12 - number;
                                                m.classList.remove(matchedLarge[0]);
                                                m.classList.add(respClass + largeClassNumber);
                                                $scope.sizeEl = largeClassNumber +"/12";
                                                initControl();
                                            }
                                        }
                                        var css = respClass + "offset-" + number;
                                        m.classList.remove(matched[0]);
                                        m.classList.add(css);
                                    }
                                } else {
                                    m.classList.add(respClass + 'offset-1');
                                    var largeClass = respClass;
                                    var large = new RegExp(largeClass + "[0-9][0-9]?");
                                    if (matchedLarge = gridClass.match(large)) {
                                        var numberLargeClass = Number(matchedLarge[0].match(/\d+/)) - 1;
                                        var maxLarge = number + numberLargeClass;
                                        if (maxLarge >= 12) {
                                            m.classList.remove(matchedLarge[0]);
                                            m.classList.add(respClass + numberLargeClass);
                                            $scope.sizeEl = numberLargeClassr +"/12";
                                            initControl();
                                        }
                                    } else {
                                        m.classList.add(respClass + '11');
                                        m.classList.add('column');
                                        m.classList.add('float-left');
                                        $scope.sizeEl = "11/12";
                                        initControl();
                                    }
                                    //rect = m.getBoundingClientRect();
                                }
                            }
                            if (left == true && cursorPosition < sidePoint - offset) {
                                sidePoint = sidePoint - offset;
                                console.log('remove offset');
                                typeClass = respClass + "offset-";
                                gridClass = m.className;
                                pattern = new RegExp(typeClass + "\\w*");
                                if (matched = gridClass.match(pattern)) {
                                    if (matched[0] != respClass + "offset-1") {
                                        number = Number(matched[0].match(/\d+/)) - 1;
                                        css = respClass + "offset-" + number;
                                        m.classList.remove(matched[0]);
                                        m.classList.add(css);
                                    } else {
                                        m.classList.remove(respClass + 'offset-1');
                                    }
                                } else {
                                    m.classList.add('float-left');
                                    console.log('here')
                                }
                            }
                            if (right == true && cursorPosition > sidePoint + offset) {
                                sidePoint = sidePoint + offset;
                                console.log('addlarge');
                                var LargeClass = respClass;
                                var gridClass = m.className;
                                var patternLarge = new RegExp(LargeClass + "[0-9][0-9]?");
                                if (matched = gridClass.match(patternLarge)) {
                                    var offsetClass = respClass + "offset-";
                                    var offsetClassPattern = new RegExp(offsetClass + "\\w*");
                                    if (matchedOffset = gridClass.match(offsetClassPattern)) {
                                        var numberOffset = Number(matchedOffset[0].match(/\d+/));
                                        var maxLargeClass = 12 - numberOffset;
                                        var numberLarge = Number(matched[0].match(/\d+/)) + 1;
                                        m.classList.remove(matched[0]);
                                        if (numberLarge - numberOffset < maxLargeClass) {
                                            m.classList.add(respClass + numberLarge);
                                            $scope.sizeEl = numberLarge +"/12"
                                        } else {
                                            m.classList.add(respClass + maxLargeClass);
                                            $scope.sizeEl = maxLargeClass +"/12";
                                            initControl();
                                        }
                                    } else if (matched[0] != respClass + "12") {
                                        var numberLarge = Number(matched[0].match(/\d+/)) + 1;
                                        var cssLarge = respClass + numberLarge;
                                        m.classList.remove(matched[0]);
                                        m.classList.add(cssLarge);
                                        $scope.sizeEl = numberLarge +"/12";
                                        initControl();
                                    }
                                }
                            }
                            if (right == true && cursorPosition < sidePoint - offset) {
                                sidePoint = sidePoint - offset;
                                console.log('remove large');
                                LargeClass = respClass;
                                gridClass = m.className;
                                patternLarge = new RegExp(LargeClass + "[0-9][0-9]?");
                                if (matchedLarge = gridClass.match(patternLarge)) {
                                    numberLarge = Number(matchedLarge[0].match(/\d+/)) - 1;
                                    if (numberLarge > 0) {
                                        css = respClass + numberLarge;
                                        console.log(css + "remove");
                                        m.classList.remove(matchedLarge[0]);
                                        m.classList.add(css);
                                        $scope.sizeEl = numberLarge +"/12";
                                        initControl();
                                    }
                                } else {
                                    m.classList.add('column');
                                    var offsetClass = respClass + "offset-";
                                    var offsetClassPattern = new RegExp(offsetClass + "\\w*");
                                    if (matchedOffset = gridClass.match(offsetClassPattern)) {
                                        var numberOffset = Number(matchedOffset[0].match(/\d+/));
                                        var maxLargeClass = 12 - numberOffset;
                                        m.classList.add(respClass + maxLargeClass);
                                        $scope.sizeEl = maxLargeClass +"/12";
                                        initControl();
                                    } else {
                                        m.classList.add(respClass + '12');
                                        m.classList.add('float-left');
                                    }
                                }
                            }
                        }
                    });*/
                }
                /*if (largeMatch = m.className.match(/large-\d+/)) {
                   $scope.newSize = largeMatch[0].match(/\d+/)[0];
                }
                if (OffsetMatch = m.className.match(/large-offset-\d+/)) {
                   $scope.newOffset = OffsetMatch[0].match(/\d+/)[0];
                }*/
            }, 100);
        }

        document.getElementById("content").addEventListener("mouseup", ActiveElement, false);
    }
    var waitForIframe = setInterval(function () {
        if (document.getElementById("content") != null) {
            wysinit();
            clearInterval(waitForIframe)
        }
    }, 200);

});