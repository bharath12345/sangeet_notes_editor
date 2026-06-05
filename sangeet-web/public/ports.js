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
   * Download a binary file (e.g., PDF).
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
  // PDF EXPORT (via fetch)
  // ===============================

  /**
   * Export PDF by fetching from server and triggering download.
   * Expects: { apiBaseUrl, composition, script, landscape, filename }
   */
  if (app.ports.exportPdf) {
    app.ports.exportPdf.subscribe(function (data) {
      const url = data.apiBaseUrl + '/export/pdf';
      const requestBody = {
        composition: data.composition,
        script: data.script,
        landscape: data.landscape || false,
      };

      fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
      })
        .then((response) => {
          if (!response.ok) {
            throw new Error('PDF export failed: ' + response.statusText);
          }
          return response.arrayBuffer();
        })
        .then((arrayBuffer) => {
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
        .catch((error) => {
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
}
