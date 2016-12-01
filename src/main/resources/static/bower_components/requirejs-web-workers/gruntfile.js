module.exports = function (grunt) {
	grunt.initConfig({
		connect: {
			server: {
				options: {
					port: 1337,
					keepalive: true,
					open: "http://localhost:1337/"
				}
			}
		}
	});

	grunt.loadNpmTasks("grunt-contrib-connect");
	grunt.registerTask("default", ["connect"]);
};