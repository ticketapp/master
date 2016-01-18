angular.module('claudeApp').
    factory('WebsitesFactory', function () {
        var factory = {
            websites : {
                iconWebsites : [],
                otherWebsites : []
            },
            normalizeWebsitesObject : function (websites, facebookUrl) {
                factory.websites = {
                    iconWebsites : [],
                    otherWebsites : []
                };
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
                        } else if (websites[i].url.indexOf('youtube') > -1) {
                            websites[i].name = 'youtube';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('instagram') > -1) {
                            websites[i].name = 'instagram';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('plus.google') > -1) {
                            websites[i].name = 'google-plus';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('pinterest') > -1) {
                            websites[i].name = 'pinterest';
                            factory.websites.iconWebsites.push(websites[i])
                        }  else if (websites[i].url.indexOf('flickr') > -1) {
                            websites[i].name = 'flickr';
                            factory.websites.iconWebsites.push(websites[i])
                        }  else if (websites[i].url.indexOf('lastfm') > -1) {
                            websites[i].name = 'lastfm';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('linkedin') > -1) {
                            websites[i].name = 'linkedin';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('tumblr') > -1) {
                            websites[i].name = 'tumblr';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('spotify') > -1) {
                            websites[i].name = 'spotify';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('vine') > -1) {
                            websites[i].name = 'vine';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('vimeo') > -1) {
                            websites[i].name = 'vimeo';
                            factory.websites.iconWebsites.push(websites[i])
                        } else if (websites[i].url.indexOf('apple') > -1) {
                            websites[i].name = 'apple';
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