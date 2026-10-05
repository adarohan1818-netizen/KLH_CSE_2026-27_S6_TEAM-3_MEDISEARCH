/* ==========================================================================
   MediSearch - frontend logic (Vanilla JavaScript)

   Data flow:
     Login -> POST /api/login -> Session check GET /api/session
     Patients -> GET /api/patients
     Search -> POST /api/search (Aho-Corasick engine on backend)
     Add Patient -> POST /api/patients (Saves data/patients/patient_xxx.txt)
   ========================================================================== */

(function () {
  'use strict';

  var state = {
    user: null,
    patients: [],
    totalRecords: 0,
    keywordCount: 0,
    matchCount: 0
  };

  /* ------------------------------ DOM helpers ----------------------------- */

  function byId(id) {
    return document.getElementById(id);
  }

  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) {
      node.className = className;
    }
    if (text !== undefined && text !== null) {
      node.textContent = String(text);
    }
    return node;
  }

  function clear(node) {
    while (node.firstChild) {
      node.removeChild(node.firstChild);
    }
  }

  function showMessage(node, text, isError) {
    node.textContent = text;
    node.className = isError ? 'message message-error' : 'message';
  }

  function hideMessage(node) {
    node.textContent = '';
    node.className = 'message hidden';
  }

  function value(row, key) {
    var raw = row[key];
    return (raw === undefined || raw === null || raw === '') ? 'Not recorded' : raw;
  }

  /* ----------------------------- AUTHENTICATION ---------------------------- */

  function checkSession() {
    fetch('/api/session')
      .then(function (res) { return res.json(); })
      .then(function (data) {
        if (data.authenticated && data.user) {
          state.user = data.user;
          showAppView();
        } else {
          showLoginView();
        }
      })
      .catch(function () {
        showLoginView();
      });
  }

  function showLoginView() {
    byId('login-view').classList.remove('hidden');
    byId('app-view').classList.add('hidden');
    hideMessage(byId('login-message'));
  }

  function showAppView() {
    byId('login-view').classList.add('hidden');
    byId('app-view').classList.remove('hidden');
    if (state.user) {
      byId('user-info').textContent = 'Doctor: ' + state.user.username;
    }
    loadPatients();
  }

  function handleLogin(event) {
    event.preventDefault();
    var form = byId('login-form');
    var username = form.elements['username'].value;
    var password = form.elements['password'].value;
    var loginBtn = byId('login-button');

    loginBtn.disabled = true;
    hideMessage(byId('login-message'));

    fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
      body: new URLSearchParams({ username: username, password: password }).toString()
    })
      .then(function (res) {
        return res.json().then(function (data) {
          return { ok: res.ok, data: data };
        });
      })
      .then(function (result) {
        loginBtn.disabled = false;
        if (!result.ok || !result.data.success) {
          showMessage(byId('login-message'), result.data.error || 'Invalid username or password.', true);
          return;
        }
        state.user = result.data.user;
        showAppView();
      })
      .catch(function () {
        loginBtn.disabled = false;
        showMessage(byId('login-message'), 'Login request failed. Ensure Java server is running.', true);
      });
  }

  function handleLogout() {
    fetch('/api/logout', { method: 'POST' })
      .then(function () {
        state.user = null;
        byId('login-form').reset();
        showLoginView();
      })
      .catch(function () {
        showLoginView();
      });
  }

  /* ------------------------------- Dashboard ------------------------------ */

  function renderStats() {
    byId('stat-patients').textContent = state.patients.length;
    byId('stat-records').textContent = state.totalRecords;
    byId('stat-keywords').textContent = state.keywordCount;
    byId('stat-matches').textContent = state.matchCount;
  }

  /* ----------------------------- Patient table ---------------------------- */

  function loadPatients() {
    fetch('/api/patients')
      .then(function (response) {
        if (response.status === 401) {
          showLoginView();
          return null;
        }
        return response.json();
      })
      .then(function (data) {
        if (!data) return;
        if (data.error) {
          showMessage(byId('patients-message'), data.error, true);
          return;
        }
        state.patients = data.patients || [];
        state.totalRecords = data.totalRecords || 0;
        renderStats();
        renderTable();
      })
      .catch(function (error) {
        console.error('Could not load patients:', error);
        showMessage(byId('patients-message'),
          'Could not load patient records. Is the Java server running?', true);
      });
  }

  function renderTable() {
    var body = byId('patients-body');
    clear(body);
    hideMessage(byId('patients-message'));

    if (state.patients.length === 0) {
      var emptyRow = el('tr');
      var emptyCell = el('td', 'table-empty',
        'No patient records found. Add a file to data/patients/ or use the Add Patient form.');
      emptyCell.colSpan = 7;
      emptyRow.appendChild(emptyCell);
      body.appendChild(emptyRow);
      return;
    }

    state.patients.forEach(function (patient) {
      var row = el('tr');
      row.appendChild(el('td', 'cell-id', value(patient, 'patientId')));
      row.appendChild(el('td', null, value(patient, 'name')));
      row.appendChild(el('td', null, value(patient, 'age')));
      row.appendChild(el('td', null, value(patient, 'gender')));
      row.appendChild(el('td', null, value(patient, 'diagnosis')));
      row.appendChild(el('td', null, value(patient, 'symptoms')));

      var actionTd = el('td');
      var viewBtn = el('button', 'btn btn-quiet btn-sm', 'View Record');
      viewBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        openPatientModal(patient);
      });
      actionTd.appendChild(viewBtn);
      row.appendChild(actionTd);

      row.addEventListener('click', function () {
        openPatientModal(patient);
      });

      body.appendChild(row);
    });
  }

  /* ------------------------------ Search flow ----------------------------- */

  function runSearch() {
    var rawInput = byId('keyword-input').value || '';
    var keywordArray = rawInput.split(',').map(function (k) { return k.trim(); }).filter(Boolean);

    clear(byId('search-results'));
    hideMessage(byId('search-message'));

    if (keywordArray.length === 0) {
      showMessage(byId('search-message'), 'Please enter at least one keyword (e.g. fever, cough, diabetes).', true);
      return;
    }

    if (keywordArray.length > 32) {
      showMessage(byId('search-message'),
        'Maximum 32 keywords supported per search (Aho-Corasick 32-bit output bitmask limitation). You entered ' + keywordArray.length + ' keywords.', true);
      return;
    }

    fetch('/api/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
      body: new URLSearchParams({ keywords: rawInput }).toString()
    })
      .then(function (response) {
        if (response.status === 401) {
          showLoginView();
          return null;
        }
        return response.json().then(function (data) {
          return { ok: response.ok, data: data };
        });
      })
      .then(function (result) {
        if (!result) return;
        if (!result.ok) {
          state.keywordCount = 0;
          state.matchCount = 0;
          renderStats();
          showMessage(byId('search-message'), result.data.error || 'The search failed.', true);
          return;
        }
        renderSearchResults(result.data);
      })
      .catch(function (error) {
        console.error('Search failed:', error);
        showMessage(byId('search-message'),
          'The search could not be completed. Is the Java server running?', true);
      });
  }

  function renderSearchResults(data) {
    var keywords = data.keywords || [];
    var results = data.results || [];

    state.keywordCount = keywords.length;
    state.matchCount = results.length;
    renderStats();

    var container = byId('search-results');
    clear(container);

    if (results.length === 0) {
      showMessage(byId('search-message'), 'No matching patient records found.', false);
      return;
    }

    showMessage(byId('search-message'),
      results.length + (results.length === 1 ? ' patient matched ' : ' patients matched ')
      + keywords.length + (keywords.length === 1 ? ' keyword' : ' keywords')
      + ': ' + keywords.join(', '), false);

    results.forEach(function (result) {
      container.appendChild(buildResultCard(result, keywords.length));
    });
  }

  function buildResultCard(result, totalKeywords) {
    var patient = result.patient || {};
    var matched = result.matchedKeywords || [];

    var card = el('div', 'result-card');

    var head = el('div', 'result-head');
    var titleGroup = el('div', 'result-title-group');
    titleGroup.appendChild(el('span', 'result-id', value(patient, 'patientId')));
    titleGroup.appendChild(el('span', 'result-name', value(patient, 'name')));
    head.appendChild(titleGroup);

    var viewBtn = el('button', 'btn btn-quiet btn-sm', 'View Record');
    viewBtn.addEventListener('click', function () {
      openPatientModal(patient);
    });
    head.appendChild(viewBtn);

    card.appendChild(head);
    card.appendChild(buildResultLine('Diagnosis', value(patient, 'diagnosis')));
    card.appendChild(buildResultLine('Symptoms', value(patient, 'symptoms')));

    card.appendChild(el('p', 'matched-label',
      'Matched keywords (' + matched.length + ' of ' + totalKeywords + ')'));

    var keywordRow = el('div', 'keyword-row');
    matched.forEach(function (keyword) {
      keywordRow.appendChild(el('span', 'keyword', keyword));
    });
    card.appendChild(keywordRow);

    return card;
  }

  function buildResultLine(label, text) {
    var line = el('p', 'result-line');
    line.appendChild(el('span', 'label', label + ': '));
    line.appendChild(document.createTextNode(text));
    return line;
  }

  /* ------------------------------ Patient modal --------------------------- */

  function openPatientModal(patient) {
    var body = byId('modal-body');
    clear(body);

    var grid = el('div', 'detail-grid');
    grid.appendChild(buildDetail('Patient ID', value(patient, 'patientId')));
    grid.appendChild(buildDetail('Name', value(patient, 'name')));
    grid.appendChild(buildDetail('Age', value(patient, 'age')));
    grid.appendChild(buildDetail('Gender', value(patient, 'gender')));
    grid.appendChild(buildDetail('Blood Group', value(patient, 'bloodGroup')));
    body.appendChild(grid);

    body.appendChild(buildDetail('Diagnosis', value(patient, 'diagnosis')));
    body.appendChild(buildDetail('Symptoms', value(patient, 'symptoms')));
    body.appendChild(buildDetail('Medications', value(patient, 'medications')));
    body.appendChild(buildDetail('Lab Reports', value(patient, 'labReports')));
    body.appendChild(buildDetail('Appointments', value(patient, 'appointments')));

    byId('modal').classList.remove('hidden');
  }

  function buildDetail(label, text) {
    var row = el('div', 'detail-row');
    row.appendChild(el('span', 'detail-label', label));
    row.appendChild(el('span', 'detail-value', text));
    return row;
  }

  function closeModal() {
    byId('modal').classList.add('hidden');
  }

  /* ------------------------------ Add patient ----------------------------- */

  var FORM_FIELDS = ['name', 'age', 'gender', 'bloodGroup', 'diagnosis',
    'symptoms', 'medications', 'labReports', 'appointments'];

  function savePatient(event) {
    event.preventDefault();

    var form = byId('add-patient-form');
    var button = byId('save-button');
    var payload = new URLSearchParams();

    FORM_FIELDS.forEach(function (field) {
      var input = form.elements[field];
      payload.append(field, input ? input.value : '');
    });

    button.disabled = true;
    hideMessage(byId('add-message'));

    fetch('/api/patients', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
      body: payload.toString()
    })
      .then(function (response) {
        if (response.status === 401) {
          showLoginView();
          return null;
        }
        return response.json().then(function (data) {
          return { ok: response.ok, data: data };
        });
      })
      .then(function (result) {
        button.disabled = false;
        if (!result) return;

        if (!result.ok) {
          showMessage(byId('add-message'), result.data.error || 'Patient could not be saved.', true);
          return;
        }

        form.reset();
        showMessage(byId('add-message'), result.data.message + ' Record created as text file.', false);
        loadPatients();
      })
      .catch(function (error) {
        button.disabled = false;
        console.error('Could not save patient:', error);
        showMessage(byId('add-message'), 'Could not save patient. Is the Java server running?', true);
      });
  }

  /* --------------------------------- Setup -------------------------------- */

  function init() {
    byId('login-form').addEventListener('submit', handleLogin);
    byId('logout-button').addEventListener('click', handleLogout);

    byId('search-button').addEventListener('click', runSearch);
    byId('clear-button').addEventListener('click', function () {
      byId('keyword-input').value = '';
      clear(byId('search-results'));
      hideMessage(byId('search-message'));
      state.keywordCount = 0;
      state.matchCount = 0;
      renderStats();
    });

    byId('keyword-input').addEventListener('keydown', function (event) {
      if (event.key === 'Enter') {
        runSearch();
      }
    });

    byId('add-patient-form').addEventListener('submit', savePatient);
    byId('modal-close').addEventListener('click', closeModal);
    byId('modal').addEventListener('click', function (event) {
      if (event.target === byId('modal')) {
        closeModal();
      }
    });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        closeModal();
      }
    });

    checkSession();
  }

  document.addEventListener('DOMContentLoaded', init);
})();
