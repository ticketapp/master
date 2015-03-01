// on déclare la fonction pour trouver les tracks d'un artsist sur echonest via son facebook 
// --> ou tu peut utiliser la fonction que tu a déjà faite à la place
function findEchonestTracks (id, start) {
    // le nombre de resutat est à 15 de base, il faut donc le monter à 100 qui est le maximum, 
    // il faudrais aussi que tu le change sur ta fonction
    $http.get('http://developer.echonest.com/api/v4/artist/songs?api_key=3ZYZKU3H3MKR2M59Z&id=facebook:artist:'+ id +
        '&format=json&start=' + start + '&results=100')
        .success(function(data) {
            // trouver chaques tracks sur youtube
            console.log('get youtube tracks');
            // si le nombre de tracks est > au nombre de tracks déjà trouvé on relance la fonction pour trouver 
            // les 100 prochaines tracks
            if (data.response.total > start + data.response.songs.length) {
                findEchonestTracks (id, start + 100)
            }
        })
}
// chercher l'artite facebook sur echonest via son id
$http.get('http://developer.echonest.com/api/v4/artist/profile?api_key=3ZYZKU3H3MKR2M59Z&id=facebook:artist:'+ 
    artistFacebook.id +'&format=json')
    .success(function (data) {
        // verifier qu'un artist à bien été trouvé
        if (data.response.artist != undefined) {
            // verifier que le nom correspond (de nombreuses erreurs sur echonest via cette methode)
            if (data.response.artist.name.toLowerCase() == artistFacebook.name.toLowerCase()) {
                // si le nom correspond chercher le titre des tracks sur echonest --> si tu utilise la fonction que 
                // tu as déjà faite, change artistFacebook par data.response.artist.id dans les arguments
                findEchonestTracks(artistFacebook.id, 0)
            } else {
                // si le nom ne correspond pas c'est peut étre que le nom facebook n'est pas le bon 
                // (parfois un "page officiel" ou autre est ajouter au nom, on compare donc les urls
                $http.get('http://developer.echonest.com/api/v4/artist/urls?api_key=3ZYZKU3H3MKR2M59Z&id=facebook:artist:' 
                    + artistFacebook.id + '&format=json')
                    .success(function (data) {
                        var wsLength = artistFacebook.websites.length;
                        var echoUrlsLength = data.response.urls.length;
                        var urlMached = 0;
                        for (var i = 0; i < wsLength; i++) {
                            // on verifie que l'url n'est pas vide
                            if (artistFacebook.websites[i] != "") {
                                // on verifie si le siteweb est matché dans les urls
                                for (var ii = 0; ii < echoUrlsLength; ii++) {
                                    if (artistFacebook.websites[i] == data.response.url[ii]) {
                                        // si le site web est matché on passe urlmatched à 1
                                        urlMached = 1;
                                        break;
                                    }
                                }
                            }
                        }
                        // si un url à été matché, on cherche les tracks sur echonest
                        if (urlMached == 1) {
                            findEchonestTracks(artistFacebook.id, 0);
                        }
                    })
            }
        } else {
            // si on ne trouve pas l'artist via son facebook, lance la recherche echonest par nom (celle que tu fait déjà)
        }
    });