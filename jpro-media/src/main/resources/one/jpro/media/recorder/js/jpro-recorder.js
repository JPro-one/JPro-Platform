let camera_stream = null;
let media_recorder = null;
let blobs_recorded = [];

enableCamera = async function(videoId, options) {
    let preview = document.getElementById(videoId);
    camera_stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
    preview.srcObject = camera_stream;

    // set MIME type of recording as video/webm
    media_recorder = new MediaRecorder(camera_stream, options);

    // event : new recorded video blob available
    media_recorder.addEventListener('dataavailable', function(e) {
        blobs_recorded.push(e.data);
        jpro.onDataavailable(e.timecode);
    });

    // event : recording stopped & all blobs sent
    media_recorder.addEventListener('stop', function() {
        // create local object URL from the recorded video blobs
        let recordedBlob = new Blob(blobs_recorded, { type: "video/webm" });
        var videoUrl = URL.createObjectURL(recordedBlob);
        jpro.onStop(videoUrl);
    });
}

startRecording = function() {
    // clear recorded buffer
    blobs_recorded = [];
    // start recording with a timeslice of 1 sec
    media_recorder.start(1000);
}

pauseRecording = function() {
    media_recorder.pause();
}

resumeRecording = function() {
    media_recorder.resume();
}

stopRecording = function() {
    media_recorder.stop();
}