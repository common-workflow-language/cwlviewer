//note: can also use requireJS in this file - see here for more info: http://requirejs.org/docs/api.html#webworker

onmessage = function (event) {
	if (event.data == "start") {
		count();
	}
}

function count() {
	var end = 1e8, tmp = 1;
	postMessage("hello there");
	while (end) {
		end -= 1;
		tmp += end;
		if (end === 5e7) { // 5e7 is the half of 1e8
			postMessage("halfway there, tmp is now " + tmp);
		}
	}
	postMessage("all done");
}