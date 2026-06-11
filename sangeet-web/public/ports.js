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
}
