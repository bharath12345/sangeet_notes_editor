/**
 * Initialize Elm ports for JavaScript interop.
 * @param {Object} app - The Elm application instance
 */
function initPorts(app) {
    // ===============================
    // WEB AUDIO PLAYBACK
    // ===============================

    let audioContext = null;
    let scheduledSources = [];

    /**
     * Initialize Web Audio context (lazy).
     */
    function getAudioContext() {
        if (!audioContext) {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        return audioContext;
    }

    /**
     * Map note + variant + octave to frequency (Hz).
     * Base: Sa (Madhya) = 261.63 Hz (middle C)
     * 12-tone equal temperament chromatic scale.
     */
    function noteToFrequency(note, variant, octave) {
        // Chromatic semitone offsets from Sa
        const noteOffsets = {
            'sa': { 'shuddha': 0 },
            're': { 'komal': 1, 'shuddha': 2 },
            'ga': { 'komal': 3, 'shuddha': 4 },
            'ma': { 'shuddha': 5, 'tivra': 6 },
            'pa': { 'shuddha': 7 },
            'dha': { 'komal': 8, 'shuddha': 9 },
            'ni': { 'komal': 10, 'shuddha': 11 }
        };

        // Octave multipliers (Madhya = 1x)
        const octaveMultipliers = {
            'atiMandra': 0.25,
            'mandra': 0.5,
            'madhya': 1.0,
            'taar': 2.0,
            'atiTaar': 4.0
        };

        const baseFreq = 261.63; // Sa (madhya) = C4
        const semitoneOffset = noteOffsets[note]?.[variant] ?? 0;
        const octaveMultiplier = octaveMultipliers[octave] ?? 1.0;

        // Frequency = base * 2^(semitones/12) * octave_multiplier
        return baseFreq * Math.pow(2, semitoneOffset / 12) * octaveMultiplier;
    }

    /**
     * Play a list of timed notes using Web Audio.
     * Each note is a triangle wave oscillator with exponential decay.
     */
    if (app.ports.playNotes) {
        app.ports.playNotes.subscribe(function(timedNotesValue) {
            const ctx = getAudioContext();
            const now = ctx.currentTime;

            // Stop any previously scheduled notes
            scheduledSources.forEach(source => {
                try {
                    source.stop();
                } catch (e) {
                    // Already stopped, ignore
                }
            });
            scheduledSources = [];

            // Parse timed notes
            const timedNotes = timedNotesValue;
            if (!Array.isArray(timedNotes)) {
                console.error("playNotes: expected array, got", timedNotes);
                return;
            }

            // Schedule each note
            timedNotes.forEach(tn => {
                const freq = noteToFrequency(tn.note, tn.variant, tn.octave);
                const startTime = now + (tn.timeMs / 1000.0);
                const duration = tn.durationMs / 1000.0;

                // Create oscillator (triangle wave for sitar-like tone)
                const osc = ctx.createOscillator();
                osc.type = 'triangle';
                osc.frequency.value = freq;

                // Create gain node for envelope
                const gainNode = ctx.createGain();
                gainNode.gain.setValueAtTime(0.3, startTime);
                // Exponential decay to simulate sitar string decay
                gainNode.gain.exponentialRampToValueAtTime(0.01, startTime + duration);

                // Connect: oscillator -> gain -> destination
                osc.connect(gainNode);
                gainNode.connect(ctx.destination);

                // Schedule start and stop
                osc.start(startTime);
                osc.stop(startTime + duration);

                scheduledSources.push(osc);
            });
        });
    }

    /**
     * Stop all currently playing/scheduled audio.
     */
    if (app.ports.stopPlayback) {
        app.ports.stopPlayback.subscribe(function() {
            scheduledSources.forEach(source => {
                try {
                    source.stop();
                } catch (e) {
                    // Already stopped, ignore
                }
            });
            scheduledSources = [];
        });
    }

    // ===============================
    // FILE DOWNLOADS
    // ===============================

    /**
     * Download a text file.
     */
    if (app.ports.downloadFile) {
        app.ports.downloadFile.subscribe(function(data) {
            const blob = new Blob([data.content], { type: data.mimeType });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = data.filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        });
    }

    /**
     * Download a binary file (e.g., PDF).
     * Expects: { filename, mimeType, bytes: Uint8Array or Array }
     */
    if (app.ports.downloadBinaryFile) {
        app.ports.downloadBinaryFile.subscribe(function(data) {
            // Convert bytes array to Uint8Array if needed
            const byteArray = data.bytes instanceof Uint8Array
                ? data.bytes
                : new Uint8Array(data.bytes);

            const blob = new Blob([byteArray], { type: data.mimeType });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = data.filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        });
    }

    // ===============================
    // PDF EXPORT (via fetch)
    // ===============================

    /**
     * Export PDF by fetching from server and triggering download.
     * Expects: { apiBaseUrl, composition, script, landscape, filename }
     */
    if (app.ports.exportPdf) {
        app.ports.exportPdf.subscribe(function(data) {
            const url = data.apiBaseUrl + '/export/pdf';
            const requestBody = {
                composition: data.composition,
                script: data.script,
                landscape: data.landscape || false
            };

            fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('PDF export failed: ' + response.statusText);
                }
                return response.arrayBuffer();
            })
            .then(arrayBuffer => {
                const blob = new Blob([arrayBuffer], { type: 'application/pdf' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = data.filename || 'composition.pdf';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(url);
            })
            .catch(error => {
                console.error('PDF export error:', error);
                alert('PDF export failed: ' + error.message);
            });
        });
    }

    // ===============================
    // FILE UPLOADS
    // ===============================

    /**
     * Open file picker and load file content.
     */
    if (app.ports.selectFile) {
        app.ports.selectFile.subscribe(function(accept) {
            const input = document.createElement('input');
            input.type = 'file';
            input.accept = accept || '.swar';

            input.addEventListener('change', function(e) {
                const file = e.target.files[0];
                if (!file) return;

                // Send filename back to Elm
                if (app.ports.fileSelected) {
                    app.ports.fileSelected.send(file.name);
                }

                // Read file content
                const reader = new FileReader();
                reader.onload = function(event) {
                    const content = event.target.result;
                    if (app.ports.fileLoaded) {
                        app.ports.fileLoaded.send(content);
                    }
                };
                reader.onerror = function() {
                    console.error("Failed to read file:", file.name);
                };
                reader.readAsText(file);
            });

            input.click();
        });
    }
}
