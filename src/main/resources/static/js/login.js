/**
 * login.js
 * Handles the login form: validation, API call, and redirect.
 */

// If user is already logged in, redirect to chat
if (localStorage.getItem('userId')) {
    window.location.href = '/chat.html';
}

const loginForm    = document.getElementById('loginForm');
const errorMsg     = document.getElementById('errorMsg');
const emailInput   = document.getElementById('email');
const passwordInput= document.getElementById('password');
const emailError   = document.getElementById('emailError');
const passwordError= document.getElementById('passwordError');
const loginBtn     = document.getElementById('loginBtn');

// ---- Client-side validation ----
function validateForm() {
    let isValid = true;

    // Clear previous errors
    emailError.textContent = '';
    passwordError.textContent = '';
    emailInput.classList.remove('input-error');
    passwordInput.classList.remove('input-error');

    if (!emailInput.value.trim()) {
        emailError.textContent = 'Email is required';
        emailInput.classList.add('input-error');
        isValid = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInput.value)) {
        emailError.textContent = 'Enter a valid email address';
        emailInput.classList.add('input-error');
        isValid = false;
    }

    if (!passwordInput.value.trim()) {
        passwordError.textContent = 'Password is required';
        passwordInput.classList.add('input-error');
        isValid = false;
    }

    return isValid;
}

// ---- Form submit handler ----
loginForm.addEventListener('submit', async function (e) {
    e.preventDefault(); // prevent default HTML form submission
    errorMsg.style.display = 'none';

    if (!validateForm()) return;

    // Disable button to prevent double-submit
    loginBtn.disabled = true;
    loginBtn.textContent = 'Signing in...';

    try {
        // Call the REST API: POST /api/users/login
        const response = await fetch('https://instant-chat-v0qf.onrender.com/api/users/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                email:    emailInput.value.trim(),
                password: passwordInput.value
            })
        });

        const data = await response.json();

        if (response.ok) {
            // Save user info to localStorage (simple session)
            localStorage.setItem('userId',   data.id);
            localStorage.setItem('username', data.username);
            localStorage.setItem('email',    data.email);

            // Redirect to chat dashboard
            window.location.href = '/chat.html';
        } else {
            // Show error returned from server
            showError(data.message || 'Login failed. Please try again.');
        }

    } catch (err) {
        showError('Cannot connect to server. Is Spring Boot running?');
    } finally {
        loginBtn.disabled = false;
        loginBtn.textContent = 'Sign In';
    }
});

function showError(message) {
    errorMsg.textContent = message;
    errorMsg.style.display = 'block';
}
