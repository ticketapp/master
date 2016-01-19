angular.module('websocketService', [
    'ngWebSocket' // you may also use 'angular-websocket' if you prefer
])
    //                          WebSocket works as well
    .factory('messagesWebsocketFactory', function($websocket) {
        // Open a WebSocket connection
        var dataStream = $websocket('wss://localhost:9000/messages');

        var collection = [];

        dataStream.onMessage(function(message) {
            collection.push(JSON.parse(message.data));
        });

        var methods = {
            collection: collection,
            get: function() {
                dataStream.send(JSON.stringify({ action: 'get' }));
            }
        };

        return methods;
    });