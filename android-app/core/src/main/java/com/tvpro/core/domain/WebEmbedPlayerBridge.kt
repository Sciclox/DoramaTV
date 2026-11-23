package com.tvpro.core.domain

object WebEmbedPlayerBridge {

    const val DEFAULT_SEEK_LOCK_MS = 4000L
    const val DEFAULT_TOGGLE_LOCK_MS = 2500L

    /**
     * Checks if the player is within the debounce window of a user-initiated seek action.
     * During this window, background sync polling must NOT overwrite currentPositionMs
     * while the web player is fetching and buffering chunks over the network.
     */
    fun isSeekLocked(
        lastSeekTimeMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        lockDurationMs: Long = DEFAULT_SEEK_LOCK_MS
    ): Boolean {
        if (lastSeekTimeMs <= 0L) return false
        return (nowMs - lastSeekTimeMs) in 0 until lockDurationMs
    }

    /**
     * Checks if the player is within the debounce window of a user-initiated play/pause toggle.
     */
    fun isPlayPauseLocked(
        lastToggleTimeMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        lockDurationMs: Long = DEFAULT_TOGGLE_LOCK_MS
    ): Boolean {
        if (lastToggleTimeMs <= 0L) return false
        return (nowMs - lastToggleTimeMs) in 0 until lockDurationMs
    }

    /**
     * Builds comprehensive JavaScript to command HTML5, JWPlayer, Video.js, ArtPlayer,
     * DPlayer, and iframed media players to play or pause.
     */
    fun buildPlayPauseJs(play: Boolean): String {
        val playBool = if (play) "true" else "false"
        val actStr = if (play) "play" else "pause"
        val cmdStr = if (play) "playVideo" else "pauseVideo"

        return """
            (function() {
                var targetPlay = $playBool;
                window._tvproUserPaused = !targetPlay;
                
                function controlJw(jp) {
                    if (!jp) return;
                    try {
                        if (targetPlay) {
                            if (typeof jp.setMute === 'function') jp.setMute(false);
                            if (typeof jp.play === 'function') jp.play(true);
                        } else {
                            if (typeof jp.pause === 'function') jp.pause();
                            if (typeof jp.play === 'function') jp.play(false);
                            if (typeof jp.setMute === 'function') jp.setMute(true);
                        }
                        if (typeof jp.setControls === 'function') jp.setControls(false);
                    } catch(e){}
                }

                function controlDocAndWin(win, doc) {
                    if (!win) return;
                    try { win._tvproUserPaused = !targetPlay; } catch(e){}

                    // 1. JWPlayer instances (check default, indexed, and named)
                    try {
                        if (win.jwplayer && typeof win.jwplayer === 'function') {
                            controlJw(win.jwplayer());
                            controlJw(win.jwplayer(0));
                            controlJw(win.jwplayer(1));
                            controlJw(win.jwplayer('a'));
                            controlJw(win.jwplayer('player'));
                            controlJw(win.jwplayer('vplayer'));
                            controlJw(win.jwplayer('myIframe'));
                        }
                    } catch(e){}

                    // 2. Video.js
                    try {
                        if (win.videojs && typeof win.videojs.getAllPlayers === 'function') {
                            var allP = win.videojs.getAllPlayers();
                            for (var pKey in allP) {
                                if (allP.hasOwnProperty(pKey) && allP[pKey]) {
                                    try {
                                        if (targetPlay) allP[pKey].play();
                                        else allP[pKey].pause();
                                    } catch(e){}
                                }
                            }
                        }
                    } catch(e){}

                    // 3. Specialty Web Players: ArtPlayer, DPlayer, Plyr, DSPlayer
                    try {
                        if (win.art) {
                            if (targetPlay && typeof win.art.play === 'function') win.art.play();
                            else if (!targetPlay && typeof win.art.pause === 'function') win.art.pause();
                        }
                        if (win.dp) {
                            if (targetPlay && typeof win.dp.play === 'function') win.dp.play();
                            else if (!targetPlay && typeof win.dp.pause === 'function') win.dp.pause();
                        }
                        if (win.dsplayer) {
                            if (targetPlay && typeof win.dsplayer.play === 'function') win.dsplayer.play();
                            else if (!targetPlay && typeof win.dsplayer.pause === 'function') win.dsplayer.pause();
                        }
                        if (win.player && typeof win.player.play === 'function') {
                            if (targetPlay) win.player.play();
                            else win.player.pause();
                        }
                    } catch(e){}

                    // 4. All HTML5 Media Elements (Video & Audio)
                    if (doc) {
                        try {
                            var mediaEls = doc.querySelectorAll('video, audio');
                            for (var i = 0; i < mediaEls.length; i++) {
                                var el = mediaEls[i];
                                try {
                                    el.controls = false;
                                    el.removeAttribute('controls');
                                    if (targetPlay) {
                                        el.muted = false;
                                        el.playbackRate = 1.0;
                                        el.play();
                                    } else {
                                        el.pause();
                                        el.playbackRate = 0.0;
                                        el.muted = true;
                                    }
                                } catch(e){}
                            }
                        } catch(e){}
                    }
                }

                // Run on main window
                controlDocAndWin(window, document);

                // Run on accessible iframes & postMessage cross-origin
                try {
                    var iframes = document.querySelectorAll('iframe');
                    for (var i = 0; i < iframes.length; i++) {
                        var ifr = iframes[i];
                        try {
                            controlDocAndWin(ifr.contentWindow, ifr.contentDocument);
                        } catch(e){}
                        try {
                            var cw = ifr.contentWindow;
                            if (cw) {
                                var act = '$actStr';
                                var actUpper = targetPlay ? 'PLAY' : 'PAUSE';
                                cw.postMessage({ type: actUpper, action: act, method: act, command: act }, '*');
                                cw.postMessage('{"command":"' + act + '"}', '*');
                                cw.postMessage('{"method":"' + act + '"}', '*');
                                cw.postMessage('{"action":"' + act + '"}', '*');
                                cw.postMessage(JSON.stringify({ event: 'command', func: '$cmdStr' }), '*');
                            }
                        } catch(e){}
                    }
                } catch(e){}
            })();
        """.trimIndent()
    }

    /**
     * Builds comprehensive JavaScript to seek HTML5, JWPlayer, Video.js, ArtPlayer,
     * DPlayer, and iframed media players to a target time in seconds.
     */
    fun buildSeekJs(targetSeconds: Double): String {
        return """
            (function() {
                var targetSec = $targetSeconds;
                window._tvproSeeking = true;
                setTimeout(function() { window._tvproSeeking = false; }, 4000);

                function seekJw(jp) {
                    if (!jp) return false;
                    try {
                        if (typeof jp.seek === 'function') {
                            jp.seek(targetSec);
                            if (typeof jp.setControls === 'function') jp.setControls(false);
                            return true;
                        }
                    } catch(e){}
                    return false;
                }

                function seekDocAndWin(win, doc) {
                    if (!win) return false;
                    var handledByPlayer = false;

                    // 1. JWPlayer instances
                    try {
                        if (win.jwplayer && typeof win.jwplayer === 'function') {
                            if (seekJw(win.jwplayer())) handledByPlayer = true;
                            if (seekJw(win.jwplayer(0))) handledByPlayer = true;
                            if (seekJw(win.jwplayer('a'))) handledByPlayer = true;
                            if (seekJw(win.jwplayer('player'))) handledByPlayer = true;
                            if (seekJw(win.jwplayer('vplayer'))) handledByPlayer = true;
                        }
                    } catch(e){}

                    // 2. Video.js
                    try {
                        if (win.videojs && typeof win.videojs.getAllPlayers === 'function') {
                            var allP = win.videojs.getAllPlayers();
                            for (var pKey in allP) {
                                if (allP.hasOwnProperty(pKey) && allP[pKey]) {
                                    try {
                                        allP[pKey].currentTime(targetSec);
                                        handledByPlayer = true;
                                    } catch(e){}
                                }
                            }
                        }
                    } catch(e){}

                    // 3. Specialty Web Players
                    try {
                        if (win.art && typeof win.art.seek === 'function') {
                            win.art.seek(targetSec);
                            handledByPlayer = true;
                        } else if (win.art && typeof win.art.currentTime !== 'undefined') {
                            win.art.currentTime = targetSec;
                            handledByPlayer = true;
                        }
                        if (win.dp && typeof win.dp.seek === 'function') {
                            win.dp.seek(targetSec);
                            handledByPlayer = true;
                        }
                        if (win.player && typeof win.player.currentTime !== 'undefined') {
                            win.player.currentTime = targetSec;
                            handledByPlayer = true;
                        }
                    } catch(e){}

                    // 4. HTML5 Media Elements (Video & Audio)
                    if (doc) {
                        try {
                            var mediaEls = doc.querySelectorAll('video');
                            for (var i = 0; i < mediaEls.length; i++) {
                                var el = mediaEls[i];
                                try {
                                    el.controls = false;
                                    el.removeAttribute('controls');
                                    el.currentTime = targetSec;
                                } catch(e){}
                            }
                        } catch(e){}
                    }

                    return handledByPlayer;
                }

                // Run on main window
                seekDocAndWin(window, document);

                // Run on accessible iframes & postMessage cross-origin
                try {
                    var iframes = document.querySelectorAll('iframe');
                    for (var i = 0; i < iframes.length; i++) {
                        var ifr = iframes[i];
                        try {
                            seekDocAndWin(ifr.contentWindow, ifr.contentDocument);
                        } catch(e){}
                        try {
                            var cw = ifr.contentWindow;
                            if (cw) {
                                cw.postMessage({
                                    type: 'SEEK',
                                    action: 'seek',
                                    method: 'seek',
                                    time: targetSec,
                                    currentTime: targetSec,
                                    position: targetSec
                                }, '*');
                                cw.postMessage(JSON.stringify({ command: 'seek', time: targetSec }), '*');
                                cw.postMessage(JSON.stringify({ method: 'seek', value: targetSec }), '*');
                                cw.postMessage(JSON.stringify({ event: 'command', func: 'seekTo', args: [targetSec, true] }), '*');
                            }
                        } catch(e){}
                    }
                } catch(e){}
            })();
        """.trimIndent()
    }
}
