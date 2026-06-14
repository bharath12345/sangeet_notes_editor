/**
 * Initialize Elm ports for JavaScript interop.
 * @param {Object} app - The Elm application instance
 */
function initPorts(app) {
  // ===============================
  // FILE DOWNLOADS
  // ===============================

  /**
   * Download a text file.
   */
  if (app.ports.downloadFile) {
    app.ports.downloadFile.subscribe(function (data) {
      var blob = new Blob([data.content], { type: data.mimeType });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = data.filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    });
  }

  /**
   * Download a binary file.
   * Expects: { filename, mimeType, bytes: Uint8Array or Array }
   */
  if (app.ports.downloadBinaryFile) {
    app.ports.downloadBinaryFile.subscribe(function (data) {
      var byteArray = data.bytes instanceof Uint8Array ? data.bytes : new Uint8Array(data.bytes);
      var blob = new Blob([byteArray], { type: data.mimeType });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = data.filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    });
  }

  // ===============================
  // FILE UPLOADS
  // ===============================

  /**
   * Open file picker and load file content.
   */
  if (app.ports.selectFile) {
    app.ports.selectFile.subscribe(function (accept) {
      var input = document.createElement('input');
      input.type = 'file';
      input.accept = accept || '.swar';

      input.addEventListener('change', function (e) {
        var file = e.target.files[0];
        if (!file) return;

        if (app.ports.fileSelected) {
          app.ports.fileSelected.send(file.name);
        }

        var reader = new FileReader();
        reader.onload = function (event) {
          var content = event.target.result;
          if (app.ports.fileLoaded) {
            app.ports.fileLoaded.send(content);
          }
        };
        reader.onerror = function () {
          console.error('Failed to read file:', file.name);
        };
        reader.readAsText(file);
      });

      input.click();
    });
  }

  // ===============================
  // CLIPBOARD
  // ===============================

  if (app.ports.copyToClipboard) {
    app.ports.copyToClipboard.subscribe(function (text) {
      navigator.clipboard.writeText(text).catch(function (err) {
        console.error('Failed to copy to clipboard:', err);
      });
    });
  }

  document.addEventListener('paste', function (e) {
    if (app.ports.clipboardContent) {
      var text = e.clipboardData.getData('text/plain');
      if (text) {
        app.ports.clipboardContent.send(text);
        e.preventDefault();
      }
    }
  });

  // ===============================
  // SECTION RENAME PROMPT
  // ===============================
  // Desktop opens a native TextInputDialog. Web has no Elm-native equivalent
  // for an in-process prompt, so we round-trip through window.prompt and send
  // the result back only if the user typed something non-empty.

  if (app.ports.requestRenameSection) {
    app.ports.requestRenameSection.subscribe(function (payload) {
      var input = window.prompt('Rename section to:', payload.currentName || '');
      if (input != null && input.trim().length > 0 && app.ports.renameSectionConfirmed) {
        app.ports.renameSectionConfirmed.send({
          sectionIndex: payload.sectionIndex,
          newName: input.trim(),
        });
      }
    });
  }

  // ===============================
  // DEBUG BRIDGE
  // ===============================
  // Activated only when the page URL has ?debug=ws://localhost:PORT.
  // Loopback-only by design: rejects anything that doesn't start with
  // ws://localhost: or ws://127.0.0.1: so a hostile page can't trick the
  // running app into shipping state to an attacker-controlled endpoint.
  // Connection lifecycle: Elm calls requestDebugConnection with the URL once
  // at boot; JS opens the socket, forwards inbound JSON messages to Elm via
  // the debugCommandReceived subscription port, and forwards Elm's outbound
  // responses (state snapshots, ack messages) over the socket.

  var debugSocket = null;

  // Loopback validation: ws:// only, localhost or 127.0.0.1, with optional port.
  // IPv6 loopback (ws://[::1]:9999) is intentionally not supported — the desktop
  // debug bridge listens on 127.0.0.1, so all tests target IPv4 loopback.
  function isLoopbackWs(url) {
    return /^ws:\/\/(localhost|127\.0\.0\.1)(:\d+)?(\/|$)/.test(url);
  }

  if (app.ports.requestDebugConnection) {
    app.ports.requestDebugConnection.subscribe(function (url) {
      if (!isLoopbackWs(url)) {
        console.warn('[debug-bridge] refusing non-loopback URL:', url);
        return;
      }
      if (debugSocket) {
        console.warn('[debug-bridge] already connected; ignoring second request');
        return;
      }
      try {
        debugSocket = new WebSocket(url);
      } catch (e) {
        console.error('[debug-bridge] WebSocket construction failed:', e);
        return;
      }
      debugSocket.onopen = function () {
        console.info('[debug-bridge] connected to', url);
      };
      debugSocket.onmessage = function (evt) {
        try {
          var parsed = JSON.parse(evt.data);
          if (app.ports.debugCommandReceived) {
            app.ports.debugCommandReceived.send(parsed);
          }
        } catch (e) {
          console.error('[debug-bridge] failed to parse incoming message:', e);
        }
      };
      debugSocket.onerror = function (e) {
        console.error('[debug-bridge] socket error:', e);
      };
      debugSocket.onclose = function () {
        console.info('[debug-bridge] socket closed');
        debugSocket = null;
      };
    });
  }

  if (app.ports.debugResponse) {
    app.ports.debugResponse.subscribe(function (payload) {
      if (debugSocket && debugSocket.readyState === WebSocket.OPEN) {
        debugSocket.send(JSON.stringify(payload));
      }
    });
  }

  // ===============================
  // CONFIG PERSISTENCE (localStorage)
  // ===============================

  var CONFIG_KEY = 'sangeet-notes-editor-config';

  if (app.ports.saveConfig) {
    app.ports.saveConfig.subscribe(function (jsonString) {
      try {
        localStorage.setItem(CONFIG_KEY, jsonString);
      } catch (err) {
        console.error('Failed to save config:', err);
      }
    });
  }

  if (app.ports.loadConfig) {
    app.ports.loadConfig.subscribe(function () {
      var stored = localStorage.getItem(CONFIG_KEY);
      if (app.ports.configLoaded) {
        app.ports.configLoaded.send(stored || '{}');
      }
    });
  }

  // ===============================
  // GOOGLE DRIVE
  // ===============================

  var driveTokenClient = null;
  var driveAccessToken = null;

  function sendDriveError(msg) {
    if (app.ports.googleDriveError) {
      app.ports.googleDriveError.send(msg);
    }
  }

  function ensureGapiLoaded(callback) {
    if (typeof gapi === 'undefined') {
      sendDriveError('Google API not loaded. Check your internet connection.');
      return;
    }
    if (gapi.client && gapi.client.drive) {
      callback();
      return;
    }
    gapi.load('client', function () {
      gapi.client
        .init({
          discoveryDocs: ['https://www.googleapis.com/discovery/v1/apis/drive/v3/rest'],
        })
        .then(callback)
        .catch(function (err) {
          sendDriveError('Failed to initialize Google API: ' + (err.message || err));
        });
    });
  }

  function withDriveAuth(callback) {
    if (driveAccessToken) {
      gapi.client.setToken({ access_token: driveAccessToken });
      callback();
    } else {
      sendDriveError('Not authenticated with Google Drive. Please connect first.');
    }
  }

  // --- Auth ---

  if (app.ports.googleDriveAuth) {
    app.ports.googleDriveAuth.subscribe(function () {
      var clientId = document.querySelector('meta[name="google-client-id"]');
      if (!clientId) {
        sendDriveError(
          'Google Client ID not configured. Add <meta name="google-client-id" content="YOUR_ID"> to index.html.',
        );
        return;
      }

      if (typeof google === 'undefined' || !google.accounts || !google.accounts.oauth2) {
        sendDriveError('Google Identity Services not loaded. Check your internet connection.');
        return;
      }

      ensureGapiLoaded(function () {
        driveTokenClient = google.accounts.oauth2.initTokenClient({
          client_id: clientId.content,
          scope: 'https://www.googleapis.com/auth/drive.file',
          callback: function (tokenResponse) {
            if (tokenResponse.error) {
              sendDriveError('Auth failed: ' + tokenResponse.error);
              if (app.ports.googleDriveAuthResult) {
                app.ports.googleDriveAuthResult.send({ success: false });
              }
              return;
            }
            driveAccessToken = tokenResponse.access_token;
            sessionStorage.setItem('gdrive_token', driveAccessToken);
            gapi.client.setToken({ access_token: driveAccessToken });
            if (app.ports.googleDriveAuthResult) {
              app.ports.googleDriveAuthResult.send({ success: true });
            }
          },
        });
        driveTokenClient.requestAccessToken();
      });
    });
  }

  // Restore token from session on load
  var savedToken = sessionStorage.getItem('gdrive_token');
  if (savedToken) {
    driveAccessToken = savedToken;
  }

  // --- List Directory ---

  if (app.ports.googleDriveListDir) {
    app.ports.googleDriveListDir.subscribe(function (folderId) {
      ensureGapiLoaded(function () {
        withDriveAuth(function () {
          var query =
            folderId === 'root'
              ? "'root' in parents and trashed = false"
              : "'" + folderId + "' in parents and trashed = false";

          gapi.client.drive.files
            .list({
              q: query,
              fields: 'files(id, name, mimeType)',
              orderBy: 'folder,name',
              pageSize: 100,
            })
            .then(function (response) {
              var files = response.result.files || [];
              if (app.ports.googleDriveDirListing) {
                app.ports.googleDriveDirListing.send({
                  folderId: folderId,
                  folderName: folderId === 'root' ? 'My Drive' : folderId,
                  items: files.map(function (f) {
                    return { id: f.id, name: f.name, mimeType: f.mimeType };
                  }),
                });
              }
            })
            .catch(function (err) {
              sendDriveError(
                'Failed to list folder: ' + (err.result ? err.result.error.message : err),
              );
            });
        });
      });
    });
  }

  // --- Read File ---

  if (app.ports.googleDriveReadFile) {
    app.ports.googleDriveReadFile.subscribe(function (fileId) {
      ensureGapiLoaded(function () {
        withDriveAuth(function () {
          gapi.client.drive.files
            .get({
              fileId: fileId,
              alt: 'media',
            })
            .then(function (response) {
              if (app.ports.googleDriveFileContent) {
                app.ports.googleDriveFileContent.send({
                  fileId: fileId,
                  content: response.body,
                });
              }
            })
            .catch(function (err) {
              sendDriveError(
                'Failed to read file: ' + (err.result ? err.result.error.message : err),
              );
            });
        });
      });
    });
  }

  // --- Write File ---

  if (app.ports.googleDriveWriteFile) {
    app.ports.googleDriveWriteFile.subscribe(function (params) {
      ensureGapiLoaded(function () {
        withDriveAuth(function () {
          var boundary = '-------sangeet_multipart';
          var delimiter = '\r\n--' + boundary + '\r\n';
          var closeDelimiter = '\r\n--' + boundary + '--';

          var metadata = JSON.stringify({ mimeType: params.mimeType });

          var multipartBody =
            delimiter +
            'Content-Type: application/json\r\n\r\n' +
            metadata +
            delimiter +
            'Content-Type: ' +
            params.mimeType +
            '\r\n\r\n' +
            params.content +
            closeDelimiter;

          gapi.client
            .request({
              path: '/upload/drive/v3/files/' + params.fileId,
              method: 'PATCH',
              params: { uploadType: 'multipart' },
              headers: {
                'Content-Type': 'multipart/related; boundary="' + boundary + '"',
              },
              body: multipartBody,
            })
            .then(function (response) {
              if (app.ports.googleDriveWriteResult) {
                app.ports.googleDriveWriteResult.send({
                  fileId: response.result.id,
                  success: true,
                });
              }
            })
            .catch(function (err) {
              sendDriveError(
                'Failed to write file: ' + (err.result ? err.result.error.message : err),
              );
            });
        });
      });
    });
  }

  // --- Create File ---

  if (app.ports.googleDriveCreateFile) {
    app.ports.googleDriveCreateFile.subscribe(function (params) {
      ensureGapiLoaded(function () {
        withDriveAuth(function () {
          var boundary = '-------sangeet_multipart';
          var delimiter = '\r\n--' + boundary + '\r\n';
          var closeDelimiter = '\r\n--' + boundary + '--';

          var metadata = JSON.stringify({
            name: params.name,
            parents: [params.parentId],
            mimeType: params.mimeType,
          });

          var multipartBody =
            delimiter +
            'Content-Type: application/json\r\n\r\n' +
            metadata +
            delimiter +
            'Content-Type: ' +
            params.mimeType +
            '\r\n\r\n' +
            params.content +
            closeDelimiter;

          gapi.client
            .request({
              path: '/upload/drive/v3/files',
              method: 'POST',
              params: { uploadType: 'multipart' },
              headers: {
                'Content-Type': 'multipart/related; boundary="' + boundary + '"',
              },
              body: multipartBody,
            })
            .then(function (response) {
              if (app.ports.googleDriveWriteResult) {
                app.ports.googleDriveWriteResult.send({
                  fileId: response.result.id,
                  success: true,
                });
              }
            })
            .catch(function (err) {
              sendDriveError(
                'Failed to create file: ' + (err.result ? err.result.error.message : err),
              );
            });
        });
      });
    });
  }

  // --- Create Folder ---

  if (app.ports.googleDriveCreateFolder) {
    app.ports.googleDriveCreateFolder.subscribe(function (params) {
      ensureGapiLoaded(function () {
        withDriveAuth(function () {
          gapi.client.drive.files
            .create({
              resource: {
                name: params.name,
                mimeType: 'application/vnd.google-apps.folder',
                parents: [params.parentId],
              },
              fields: 'id, name, mimeType',
            })
            .then(function (response) {
              if (app.ports.googleDriveWriteResult) {
                app.ports.googleDriveWriteResult.send({
                  fileId: response.result.id,
                  success: true,
                });
              }
              // Refresh the parent folder listing
              if (app.ports.googleDriveListDir) {
                app.ports.googleDriveListDir.subscribe._last_parentId = params.parentId;
              }
            })
            .catch(function (err) {
              sendDriveError(
                'Failed to create folder: ' + (err.result ? err.result.error.message : err),
              );
            });
        });
      });
    });
  }

  // --- Rename Item ---

  if (app.ports.googleDriveRenameItem) {
    app.ports.googleDriveRenameItem.subscribe(function (params) {
      ensureGapiLoaded(function () {
        withDriveAuth(function () {
          gapi.client.drive.files
            .update({
              fileId: params.fileId,
              resource: { name: params.newName },
            })
            .then(function () {
              if (app.ports.googleDriveWriteResult) {
                app.ports.googleDriveWriteResult.send({
                  fileId: params.fileId,
                  success: true,
                });
              }
            })
            .catch(function (err) {
              sendDriveError('Failed to rename: ' + (err.result ? err.result.error.message : err));
            });
        });
      });
    });
  }

  // --- Delete Item ---

  if (app.ports.googleDriveDeleteItem) {
    app.ports.googleDriveDeleteItem.subscribe(function (fileId) {
      ensureGapiLoaded(function () {
        withDriveAuth(function () {
          gapi.client.drive.files
            .update({
              fileId: fileId,
              resource: { trashed: true },
            })
            .then(function () {
              if (app.ports.googleDriveWriteResult) {
                app.ports.googleDriveWriteResult.send({
                  fileId: fileId,
                  success: true,
                });
              }
            })
            .catch(function (err) {
              sendDriveError('Failed to delete: ' + (err.result ? err.result.error.message : err));
            });
        });
      });
    });
  }

  // --- Move Item ---

  if (app.ports.googleDriveMoveItem) {
    app.ports.googleDriveMoveItem.subscribe(function (params) {
      ensureGapiLoaded(function () {
        withDriveAuth(function () {
          // Get current parents first
          gapi.client.drive.files
            .get({
              fileId: params.fileId,
              fields: 'parents',
            })
            .then(function (response) {
              var previousParents = (response.result.parents || []).join(',');
              return gapi.client.drive.files.update({
                fileId: params.fileId,
                addParents: params.newParentId,
                removeParents: previousParents,
              });
            })
            .then(function () {
              if (app.ports.googleDriveWriteResult) {
                app.ports.googleDriveWriteResult.send({
                  fileId: params.fileId,
                  success: true,
                });
              }
            })
            .catch(function (err) {
              sendDriveError('Failed to move: ' + (err.result ? err.result.error.message : err));
            });
        });
      });
    });
  }

  // ============================================================================
  // ANALYTICS (PostHog)
  // ============================================================================
  // PostHog is initialized in index.html with autocapture disabled so we get
  // clean, intentional events instead of every DOM mutation. Here we wire two
  // global capture handlers — clicks and keystrokes — tagged with the UI region
  // they happened in. Region is derived from existing CSS class names on the
  // top-level containers, so no Elm view changes are needed. Unknown regions
  // fall back to "unknown" (catchable in dashboards).

  // Match the most-specific selector first; closest() walks up the DOM until
  // it hits one. Updates to the UI shell may require adding entries here, but
  // a missing entry just yields "unknown" — graceful degradation.
  var REGION_SELECTORS = [
    { selector: '.toolbar', region: 'toolbar' },
    { selector: '#status-bar, .status-bar', region: 'status-bar' },
    { selector: '.file-browser-panel', region: 'file-browser' },
    { selector: '.editor-header', region: 'editor-header' },
    { selector: '.canvas-area, .canvas-area-with-legend', region: 'editor' },
    { selector: '.modal, .dialog, [role="dialog"]', region: 'dialog' },
  ];

  function detectRegion(el) {
    if (!el || !el.closest) return 'unknown';
    for (var i = 0; i < REGION_SELECTORS.length; i++) {
      if (el.closest(REGION_SELECTORS[i].selector)) {
        return REGION_SELECTORS[i].region;
      }
    }
    return 'unknown';
  }

  function detectElement(target) {
    if (!target || !target.closest) return 'unknown';
    // Explicit data-element wins if any element has bothered to set it.
    var explicit = target.closest('[data-element]');
    if (explicit) return explicit.dataset.element;
    // Fall back to nearest button/link/role=button + its label.
    var btn = target.closest('button, a, [role="button"]');
    if (btn) {
      var text = (btn.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 40);
      var tag = btn.tagName.toLowerCase();
      return text ? tag + ':' + text : tag;
    }
    return target.tagName ? target.tagName.toLowerCase() : 'unknown';
  }

  function safeCapture(name, props) {
    try {
      if (window.posthog && typeof window.posthog.capture === 'function') {
        window.posthog.capture(name, props);
      }
    } catch (e) {
      // never let analytics throw into the app
    }
  }

  document.addEventListener(
    'click',
    function (e) {
      safeCapture('click', {
        region: detectRegion(e.target),
        element: detectElement(e.target),
      });
    },
    { capture: true },
  );

  // Modifier-only keys (Shift / Ctrl / etc.) are noise — drop them. Auto-repeat
  // events from a held key (e.repeat=true) are also dropped so a one-second
  // hold doesn't spam dozens of identical events. A small additional debounce
  // catches paste-burst-style flurries.
  var MODIFIER_KEYS = { Shift: 1, Control: 1, Alt: 1, Meta: 1, CapsLock: 1, AltGraph: 1 };
  var lastKeyAt = 0;
  document.addEventListener(
    'keydown',
    function (e) {
      if (e.repeat) return;
      if (MODIFIER_KEYS[e.key]) return;
      var now = Date.now();
      if (now - lastKeyAt < 25) return;
      lastKeyAt = now;
      safeCapture('keystroke', {
        key: e.key,
        region: detectRegion(document.activeElement),
        ctrl: e.ctrlKey,
        meta: e.metaKey,
        shift: e.shiftKey,
        alt: e.altKey,
      });
    },
    { capture: true },
  );

  // ============================================================================
  // REPLAY BUFFER (rrweb)
  // ============================================================================
  // rrweb records a continuous stream of DOM mutations + input events into an
  // in-memory ring buffer. Nothing leaves the browser until the user clicks
  // "Report a Bug" (Phase 4b — not built yet). The buffer is intentionally
  // RAM-only — never persisted to localStorage — so it dies with the tab.
  //
  // Eviction policy: time-based (last 5 minutes) AND size-capped (10 MB hard
  // limit on serialized JSON). The time bound is the primary signal; the size
  // bound is defensive against rare editing-heavy bursts that would otherwise
  // OOM the tab.
  //
  // Dev hooks (window.__replay):
  //   __replay.events()    → snapshot copy of the current buffer
  //   __replay.stats()     → { count, ageMs, sizeBytes }
  //   __replay.clear()     → empty the buffer (for testing)

  var REPLAY_MAX_AGE_MS = 5 * 60 * 1000;
  var REPLAY_MAX_BYTES = 10 * 1024 * 1024;
  var replayBuffer = [];
  var replayBytes = 0;

  function trimReplay() {
    var cutoff = Date.now() - REPLAY_MAX_AGE_MS;
    // Drop expired events from the head (rrweb timestamps monotonically).
    while (replayBuffer.length > 0 && replayBuffer[0].timestamp < cutoff) {
      replayBytes -= replayBuffer[0].__sz || 0;
      replayBuffer.shift();
    }
    // If still over the byte budget, keep dropping oldest until under.
    while (replayBuffer.length > 0 && replayBytes > REPLAY_MAX_BYTES) {
      replayBytes -= replayBuffer[0].__sz || 0;
      replayBuffer.shift();
    }
  }

  if (typeof rrweb !== 'undefined' && typeof rrweb.record === 'function') {
    try {
      rrweb.record({
        emit: function (event) {
          // Track per-event size by stringifying once on ingest. Approximate but
          // accurate enough for the byte cap — and far cheaper than
          // re-stringifying the whole buffer on every emit.
          try {
            event.__sz = JSON.stringify(event).length;
          } catch (e) {
            event.__sz = 0;
          }
          replayBuffer.push(event);
          replayBytes += event.__sz;
          trimReplay();
        },
        // Mask password fields defensively even though we don't have any.
        // Other inputs (composition title, sahitya text) are intentionally
        // captured — they're the user's own creative content and the replay
        // is only sent on their explicit consent.
        maskAllInputs: false,
        maskInputOptions: { password: true },
      });
    } catch (e) {
      console.warn('rrweb recorder failed to start:', e);
    }
  } else {
    console.warn('rrweb library not loaded — replay buffer disabled');
  }

  window.__replay = {
    events: function () {
      // Strip the internal __sz field on read so callers don't see it.
      return replayBuffer.map(function (e) {
        var copy = {};
        for (var k in e) if (k !== '__sz') copy[k] = e[k];
        return copy;
      });
    },
    stats: function () {
      var ageMs = replayBuffer.length > 0 ? Date.now() - replayBuffer[0].timestamp : 0;
      return {
        count: replayBuffer.length,
        ageMs: ageMs,
        sizeBytes: replayBytes,
      };
    },
    clear: function () {
      replayBuffer = [];
      replayBytes = 0;
    },
  };

  // ============================================================================
  // BUG REPORT SUBMIT (Phase 4b)
  // ============================================================================
  // Elm sends { description, email, apiBaseUrl }. JS bundles the rrweb replay
  // buffer + browser metadata and POSTs to {apiBaseUrl}/bug-reports, then sends
  // a { success, message } result back via the inbound bugReportResult port.
  // The replay buffer travels through JS (not Elm) because it can be several MB
  // — avoids two extra JSON serialization passes through the Elm runtime.

  function sendBugReportResult(success, message) {
    if (app.ports.bugReportResult) {
      app.ports.bugReportResult.send({ success: success, message: message });
    }
  }

  if (app.ports.submitBugReport) {
    app.ports.submitBugReport.subscribe(function (data) {
      var url = (data.apiBaseUrl || '').replace(/\/$/, '') + '/bug-reports';
      var payload = {
        type: 'web',
        description: data.description,
        email: data.email || null,
        replay: window.__replay ? window.__replay.events() : [],
        metadata: {
          url: window.location.href,
          userAgent: navigator.userAgent,
          viewportW: window.innerWidth,
          viewportH: window.innerHeight,
          timestamp: new Date().toISOString(),
        },
      };
      fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
        .then(function (resp) {
          if (!resp.ok) {
            return resp.text().then(function (body) {
              throw new Error('HTTP ' + resp.status + ': ' + body.slice(0, 200));
            });
          }
          return resp.json();
        })
        .then(function (body) {
          var reportId = (body && body.reportId) || 'unknown';
          sendBugReportResult(true, 'report id ' + reportId);
        })
        .catch(function (err) {
          sendBugReportResult(false, (err && err.message) || String(err));
        });
    });
  }
}
