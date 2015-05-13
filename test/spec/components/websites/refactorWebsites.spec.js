'use strict';
describe('Factory: WebsitesFactory', function () {
    var WebsitesFactory;

    beforeEach(module('claudeApp'));
    beforeEach(inject(function (_WebsitesFactory_) {
        WebsitesFactory = _WebsitesFactory_;
    }));

    it('should refactore websites', function () {
        var websites = ['www.jkjkjk/facebook.com', 'www.jljl/twitter.fr', 'http://abc/soundcloud.com', 'www.hjhhjh.com'];
        var facebookUrl = 'lklklklklklk';
        var normalizedWebsites = WebsitesFactory.normalizeWebsitesObject(websites, facebookUrl);

        var expectedWebsites = {
            iconWebsites : [
                {
                    url : 'www.jkjkjk/facebook.com',
                    name : 'facebook'
                },
                {
                    url : 'www.jljl/twitter.fr',
                    name : 'twitter'
                },
                {
                    url : 'http://abc/soundcloud.com',
                    name : 'soundcloud'
                }
            ],
            otherWebsites : [
                {
                    url : 'www.hjhhjh.com',
                    name : 'website'
                }
            ],
            facebookUrl : 'lklklklklklk'
        };

        expect(normalizedWebsites).toEqual(expectedWebsites);
    });
});