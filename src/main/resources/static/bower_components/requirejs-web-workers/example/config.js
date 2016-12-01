var require = (function (window) {
	return {
		//by default load modules from src
		baseUrl: "/src",
		paths: {
			//except if the module ID starts with "app"
			app: "../example",

			//CDN
			"jquery": "//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min"
		}
	};
})(this);