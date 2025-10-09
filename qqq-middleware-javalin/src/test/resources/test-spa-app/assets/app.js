// Test SPA App JavaScript
console.log('Test SPA App loaded');

// Simulate client-side routing
window.addEventListener('DOMContentLoaded', function() {
    const path = window.location.pathname;
    document.getElementById('app-root').innerHTML += '<p>Current path: ' + path + '</p>';
});
