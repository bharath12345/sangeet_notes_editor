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
   * Download a binary file.
   * Expects: { filename, mimeType, bytes: Uint8Array or Array }
   */
  if (app.ports.downloadBinaryFile) {
    app.ports.downloadBinaryFile.subscribe(function (data) {
      // Convert bytes array to Uint8Array if needed
      const byteArray = data.bytes instanceof Uint8Array ? data.bytes : new Uint8Array(data.bytes);

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
  // FILE UPLOADS
  // ===============================

  /**
   * Open file picker and load file content.
   */
  if (app.ports.selectFile) {
    app.ports.selectFile.subscribe(function (accept) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = accept || '.swar';

      input.addEventListener('change', function (e) {
        const file = e.target.files[0];
        if (!file) return;

        // Send filename back to Elm
        if (app.ports.fileSelected) {
          app.ports.fileSelected.send(file.name);
        }

        // Read file content
        const reader = new FileReader();
        reader.onload = function (event) {
          const content = event.target.result;
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

  /**
   * Copy text to system clipboard.
   */
  if (app.ports.copyToClipboard) {
    app.ports.copyToClipboard.subscribe(function (text) {
      navigator.clipboard.writeText(text).catch(function (err) {
        console.error('Failed to copy to clipboard:', err);
      });
    });
  }

  /**
   * Listen for paste events and send clipboard content to Elm.
   */
  document.addEventListener('paste', function (e) {
    if (app.ports.clipboardContent) {
      const text = e.clipboardData.getData('text/plain');
      if (text) {
        app.ports.clipboardContent.send(text);
        e.preventDefault();
      }
    }
  });
}
