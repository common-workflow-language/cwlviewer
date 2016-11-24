#Using Web Workers with RequireJS

This is a simple plugin that allows you to declare a dependency on a web worker script using requireJS with a simple syntax. See here for [primer on Web Workers](https://developer.mozilla.org/en-US/docs/DOM/Using_web_workers).

```javascript
    define(["worker!my-web-worker"], function(worker) {
		worker.onmessage = function (event) {
			alert("I got me a message!");
		};
	});
```

The plugin will return an initialized `Worker` object which will resolve the given module ID with the currently configured requireJS configuration (no need to hardcode script paths just for web workers).

If Worker is not defined (IE < 10), the plugin will load a [fake Worker implementation](http://code.google.com/p/fakeworker-js/) so that your scripts can utilize the same Worker API whether the browser supports it or not.

##Install with [Bower](http://bower.io/)

```
bower install requirejs-web-workers
```

Then add `src/worker.js` and `src/worker-fake.js` to your project.

##How to Run Example Page

The example page just loops and counts to a very large number on a background thread. In order to run the example, you will need to run index.html from a server (e.g. `localhost`) rather than the `file://` protocol for web workers to work.

To run the example with a lightweight node server, first install dependencies:

```
npm install
```

Make sure you have grunt-cli installed if you don't already have it:

```
npm install grunt-cli -g
```

Then, run the server and load the example page:

```
npm start
```

This will spin up a server at `localhost:1337` and open your web browser.
