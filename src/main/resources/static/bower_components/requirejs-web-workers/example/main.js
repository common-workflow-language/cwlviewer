define(["jquery", "worker!app/counting"], function ($, counter) {
	counter.onmessage = function (event) {
		$("#results").append("<li>message from the background thread: <strong>" + event.data + "</strong></li>");
	};

	$("#doIt").click(function () {
		counter.postMessage("start");
	});
});