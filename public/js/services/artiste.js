	app.factory ('ArtisteFactory', function ($http, $q){
		var factory = {
			artistes : false,		
		getArtistes : function(){
		var deferred = $q.defer();
		if(factory.artistes ==true){
		deferred.resolve(factory.artistes);
		}else{
		$http.get('/artists')
		.success(function(data, status){
			factory.artistes = data;
			deferred.resolve(factory.artistes);
			}).error(function(data, status){
				deferred.reject('erreur');
			});
		}
		return deferred.promise;
		},
		getArtiste : function (id){
		var deferred = $q.defer();
		var artistes = {};
		var artistes = factory.getArtistes().then(function(artistes){
			angular.forEach(factory.artistes, function(value, key){
			if(value.id == id){
				artiste = value
				}
				});
				deferred.resolve(artiste);
			}, function(msg){
			deferred.reject(msg);
	});
		return deferred.promise;
		}
		};
		return factory;
	});
	