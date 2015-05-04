angular.module('claudeApp').
    factory('WebsitesFactory', function () {
        var factory = {
            websites : {
                iconWebsites : [],
                otherWebsites : []
            },
            normalizeWebsitesObject : function (websites, facebookUrl) {
                for (var i = 0; i < websites.length; i++) {
                    websites[i] = {url: websites[i]};
                    if (websites[i].url.length > 0) {
                        if (websites[i].url.indexOf('facebook') > -1) {
                            websites[i].name = 'facebook';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('twitter') > -1) {
                            websites[i].name = 'twitter';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('soundcloud') > -1) {
                            websites[i].name = 'soundcloud';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('mixcloud') > -1) {
                            websites[i].name = 'mixcloud';
                            factory.websites.iconWebsites.push(websites[i])
                        } else {
                            websites[i].name = 'website';
                            factory.websites.otherWebsites.push(websites[i])
                        }
                    }
                }
                factory.websites.facebookUrl = facebookUrl;
                return factory.websites
            }
        };
        return factory
    });