const API_BASE = 'http://localhost:8080/api';

// On load
document.addEventListener('DOMContentLoaded', () => {
    loadAllPatients();
});

// Tab Navigation
function showTab(tabId) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.style.display = 'none';
        tab.classList.remove('active');
    });
    
    // Deactivate all buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected tab
    const selectedTab = document.getElementById(tabId);
    if(selectedTab) {
        selectedTab.style.display = 'block';
        selectedTab.classList.add('active');
    }
    
    // Activate clicked button
    const btn = Array.from(document.querySelectorAll('.tab-btn')).find(b => b.getAttribute('onclick').includes(tabId));
    if(btn) btn.classList.add('active');
}

// Fetch All Patients
async function loadAllPatients() {
    try {
        const response = await fetch(`${API_BASE}/patients`);
        const data = await response.json();
        
        // Update stats
        document.getElementById('total-patients-count').innerText = data.length;
        
        // Update list
        const container = document.getElementById('all-patients-list');
        renderPatients(data, container, null);
    } catch (error) {
        console.error('Error fetching patients:', error);
        document.getElementById('total-patients-count').innerText = 'Error';
    }
}

// Single Keyword Search
async function performSearch(algorithm) {
    const query = document.getElementById('single-query').value.trim();
    const container = document.getElementById('single-results');
    
    if (!query) {
        container.innerHTML = '<div class="no-results">Please enter a keyword to search.</div>';
        return;
    }

    container.innerHTML = '<div>Searching...</div>';

    try {
        const endpoint = algorithm === 'kmp' ? 'kmp' : 'rabinkarp';
        const response = await fetch(`${API_BASE}/search/${endpoint}?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        
        let badgeInfo = null;
        if (algorithm === 'kmp') badgeInfo = { class: 'kmp', text: 'Algorithm Used: KMP' };
        else badgeInfo = { class: 'rabin-karp', text: 'Algorithm Used: Rabin-Karp' };

        renderPatients(data, container, badgeInfo);
    } catch (error) {
        console.error('Search error:', error);
        container.innerHTML = '<div class="no-results">Error performing search. Is the Java backend running?</div>';
    }
}

// Multi-Keyword Search (Aho-Corasick)
async function performMultiSearch() {
    const query = document.getElementById('multi-query').value.trim();
    const container = document.getElementById('multi-results');
    
    if (!query) {
        container.innerHTML = '<div class="no-results">Please enter keywords separated by comma.</div>';
        return;
    }

    container.innerHTML = '<div>Searching...</div>';

    try {
        const response = await fetch(`${API_BASE}/search/aho?keywords=${encodeURIComponent(query)}`);
        const data = await response.json();
        
        if (data.length === 0) {
            container.innerHTML = '<div class="no-results">No matches found.</div>';
            return;
        }

        container.innerHTML = '';
        data.forEach(result => {
            container.innerHTML += buildPatientCard(
                result.patient, 
                { class: 'aho-corasick', text: 'Algorithm: Aho-Corasick' },
                `<p><strong>Keywords Found:</strong> ${result.foundKeywords.join(', ')}</p>`
            );
        });
    } catch (error) {
        console.error('Search error:', error);
        container.innerHTML = '<div class="no-results">Error performing search.</div>';
    }
}

// Similar Case Search (Edit Distance)
async function performSimilarSearch() {
    const query = document.getElementById('similar-query').value.trim();
    const container = document.getElementById('similar-results');
    
    if (!query) {
        container.innerHTML = '<div class="no-results">Please enter a case description.</div>';
        return;
    }

    container.innerHTML = '<div>Calculating Edit Distances...</div>';

    try {
        const response = await fetch(`${API_BASE}/search/similar?query=${encodeURIComponent(query)}`);
        const data = await response.json();
        
        if (data.length === 0) {
            container.innerHTML = '<div class="no-results">No similar cases found within threshold.</div>';
            return;
        }

        container.innerHTML = '';
        data.forEach(result => {
            container.innerHTML += buildPatientCard(
                result.patient, 
                { class: 'edit-distance', text: 'Algorithm: Edit Distance' },
                `<p><strong>Similarity Status:</strong> ${result.status}</p>
                 <p><strong>Edit Distance:</strong> ${result.distance}</p>`
            );
        });
    } catch (error) {
        console.error('Search error:', error);
        container.innerHTML = '<div class="no-results">Error performing search.</div>';
    }
}

// Helper to render basic patient list
function renderPatients(patients, container, badgeInfo) {
    if (patients.length === 0) {
        container.innerHTML = '<div class="no-results">No patients found.</div>';
        return;
    }
    
    container.innerHTML = '';
    patients.forEach(p => {
        container.innerHTML += buildPatientCard(p, badgeInfo, '');
    });
}

// HTML builder for patient card
function buildPatientCard(patient, badgeInfo, extraHtml) {
    let badgeHtml = '';
    if (badgeInfo) {
        badgeHtml = `<span class="badge ${badgeInfo.class}">${badgeInfo.text}</span>`;
    }

    return `
        <div class="card patient-card">
            <div class="patient-header">
                <h3>${patient.name} (${patient.id})</h3>
                ${badgeHtml}
            </div>
            <div class="patient-details">
                <p><strong>Age:</strong> ${patient.age}</p>
                <p><strong>Diagnosis:</strong> ${patient.diagnosis}</p>
                <p><strong>Prescription:</strong> ${patient.prescription}</p>
                <p><strong>Lab Report:</strong> ${patient.labReport}</p>
                <p><strong>History:</strong> ${patient.appointmentHistory}</p>
                ${extraHtml}
            </div>
        </div>
    `;
}
