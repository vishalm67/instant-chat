/**
 * register.js
 * Handles the registration form: validation, API call, and redirect.
 */

// If already logged in, go to chat
if (localStorage.getItem('userId')) {
    window.location.href = '/chat.html';
}

const registerForm  = document.getElementById('registerForm');
const errorMsg      = document.getElementById('errorMsg');
const successMsg    = document.getElementById('successMsg');
const usernameInput = document.getElementById('username');
const emailInput    = document.getElementById('email');
const passwordInput = document.getElementById('password');
const usernameError = document.getElementById('usernameError');
const emailError    = document.getElementById('emailError');
const passwordError = document.getElementById('passwordError');
const registerBtn   = document.getElementById('registerBtn');

// ---- Client-side validation ----
function validateForm() {
    let isValid = true;

    // Clear previous errors
    usernameError.textContent = '';
    emailError.textContent    = '';
    passwordError.textContent = '';
    usernameInput.classList.remove('input-error');
    emailInput.classList.remove('input-error');
    passwordInput.classList.remove('input-error');

    if (!usernameInput.value.trim()) {
        usernameError.textContent = 'Username is required';
        usernameInput.classList.add('input-error');
        isValid = false;
    } else if (usernameInput.value.trim().length < 3) {
        usernameError.textContent = 'Username must be at least 3 characters';
        usernameInput.classList.add('input-error');
        isValid = false;
    }

    if (!emailInput.value.trim()) {
        emailError.textContent = 'Email is required';
        emailInput.classList.add('input-error');
        isValid = false;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInput.value)) {
        emailError.textContent = 'Enter a valid email address';
        emailInput.classList.add('input-error');
        isValid = false;
    }

    if (!passwordInput.value) {
        passwordError.textContent = 'Password is required';
        passwordInput.classList.add('input-error');
        isValid = false;
    } else if (passwordInput.value.length < 6) {
        passwordError.textContent = 'Password must be at least 6 characters';
        passwordInput.classList.add('input-error');
        isValid = false;
    }

    return isValid;
}

// ---- Form submit handler ----
registerForm.addEventListener('submit', async function (e) {
    e.preventDefault();

    // Hide previous messages
    errorMsg.style.display   = 'none';
    successMsg.style.display = 'none';

    if (!validateForm()) return;

    registerBtn.disabled  = true;
    registerBtn.textContent = 'Creating account...';

    try {
        // Call REST API: POST /api/users/register
        const response = await fetch('/api/users/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: usernameInput.value.trim(),
                email:    emailInput.value.trim(),
                password: passwordInput.value
            })
        });

        const data = await response.json();

        if (response.status === 201) {
            // Show success message, then redirect to login
            successMsg.textContent   = 'Account created! Redirecting to login...';
            successMsg.style.display = 'block';
            registerForm.reset();

            setTimeout(() => {
                window.location.href = '/index.html';
            }, 2000);

        } else {
            // Show server error (duplicate username, email, etc.)
            showError(data.message || 'Registration failed.');
        }

    } catch (err) {
        showError('Cannot connect to server. Is Spring Boot running?');
    } finally {
        registerBtn.disabled    = false;
        registerBtn.textContent = 'Create Account';
    }
});

function showError(message) {
    errorMsg.textContent   = message;
    errorMsg.style.display = 'block';
}
