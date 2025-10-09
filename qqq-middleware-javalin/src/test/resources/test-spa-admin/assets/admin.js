// Test SPA Admin JavaScript
console.log('Test SPA Admin loaded');

// Simulate client-side routing
window.addEventListener('DOMContentLoaded', function() {
    const path = window.location.pathname;
    document.getElementById('admin-root').innerHTML += '<p>Current path: ' + path + '</p>';
});
