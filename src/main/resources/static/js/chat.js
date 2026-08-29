/**
 * chat.js
 * Main chat dashboard logic:
 * - Load user list from REST API
 * - Connect to WebSocket via STOMP
 * - Send & receive messages in real time
 * - Display conversation history
 */

// =============================================
// 1. AUTH GUARD
// =============================================
// If not logged in, redirect to login page
const currentUserId  = localStorage.getItem('userId');
const currentUsername= localStorage.getItem('username');

if (!currentUserId) {
    window.location.href = '/index.html';
}

// =============================================
// 2. DOM REFERENCES
// =============================================
const currentUsernameEl = document.getElementById('currentUsername');
const currentUserAvatar = document.getElementById('currentUserAvatar');
const userList          = document.getElementById('userList');
const searchInput       = document.getElementById('searchUsers');
const logoutBtn         = document.getElementById('logoutBtn');
const noChatSelected    = document.getElementById('noChatSelected');
const chatWindow        = document.getElementById('chatWindow');
const chatUsername      = document.getElementById('chatUsername');
const chatUserAvatar    = document.getElementById('chatUserAvatar');
const connectionStatus  = document.getElementById('connectionStatus');
const messagesArea      = document.getElementById('messagesArea');
const messageInput      = document.getElementById('messageInput');
const charCount         = document.getElementById('charCount');
const sendBtn           = document.getElementById('sendBtn');

// =============================================
// 3. STATE VARIABLES
// =============================================
let stompClient     = null;   // WebSocket/STOMP connection
let selectedUser    = null;   // Currently selected user object {id, username, email}
let unreadCounts = {}; // { senderId: count }
let allUsers        = [];     // All registered users (excluding current user)
let onlineUsers = new Set();
// =============================================
// 4. INITIALIZE DASHBOARD
// =============================================
function init() {
    currentUsernameEl.textContent = currentUsername;
    currentUserAvatar.textContent = currentUsername.charAt(0).toUpperCase();

    // Step 1: load users first, THEN connect websocket
    loadUsers().then(() => {
        connectWebSocket();
    });
}
// =============================================
// 5. LOAD USERS FROM REST API
// =============================================
async function loadUsers() {
    try {
        const usersRes   = await fetch('https://instant-chat-v0qf.onrender.com/api/users');
        const users      = await usersRes.json();

        const onlineRes  = await fetch('https://instant-chat-v0qf.onrender.com/api/users/online');
        const onlineIds  = await onlineRes.json();
        onlineUsers      = new Set(onlineIds.map(id => String(id)));

        // Fetch unread counts
        const unreadRes  = await fetch(`http://localhost:8080/api/messages/unread?userId=${currentUserId}`);
        unreadCounts     = await unreadRes.json();

        allUsers = users.filter(u => String(u.id) !== String(currentUserId));
        renderUserList(allUsers);

    } catch (err) {
        console.error('Failed to load users:', err);
        userList.innerHTML = '<li style="color:#f87171;padding:16px;font-size:14px;">Failed to load users</li>';
    }
}
// =============================================
// 6. RENDER USER LIST IN SIDEBAR
// =============================================
function renderUserList(users) {
    userList.innerHTML = '';

    if (users.length === 0) {
        userList.innerHTML = '<li style="color:#64748b;padding:16px;font-size:14px;">No other users yet</li>';
        return;
    }

    users.forEach(user => {
        const isOnline  = onlineUsers.has(String(user.id));
        const unread    = unreadCounts[user.id] || 0;

        const li = document.createElement('li');
        li.className = 'user-item';
        li.dataset.userId = user.id;

        li.innerHTML = `
            <div class="avatar">${user.username.charAt(0).toUpperCase()}</div>
            <div class="user-info">
                <div class="username">${escapeHtml(user.username)}</div>
                <div class="user-status ${isOnline ? 'status-online' : 'status-offline'}">
                    ${isOnline ? '● Online' : '● Offline'}
                </div>
            </div>
            ${unread > 0 ? `<div class="unread-badge">${unread}</div>` : ''}
        `;

        li.addEventListener('click', () => selectUser(user));

        if (selectedUser && String(selectedUser.id) === String(user.id)) {
            li.classList.add('active');
        }

        userList.appendChild(li);
    });
}
// =============================================
// 7. SELECT A USER (open chat)
// =============================================
function selectUser(user) {
    selectedUser = user;

    // Highlight selected user in sidebar
    document.querySelectorAll('.user-item').forEach(li => li.classList.remove('active'));
    const activeLi = document.querySelector(`.user-item[data-user-id="${user.id}"]`);
    if (activeLi) activeLi.classList.add('active');

    // Update chat header
    chatUsername.textContent  = user.username;
    chatUserAvatar.textContent= user.username.charAt(0).toUpperCase();

    // Show chat window
    noChatSelected.style.display = 'none';
    chatWindow.style.display     = 'flex';

    // Clear messages and load history
    messagesArea.innerHTML = '';
    loadMessageHistory(user.id);

  // ---- NEW: clear unread badge when opening chat ----
    clearUnreadBadge(user.id);
    // Focus the input box
    messageInput.focus();
}

// =============================================
// 8. LOAD MESSAGE HISTORY (REST API)
// =============================================
async function loadMessageHistory(otherUserId) {
    try {
        // GET /api/messages/{otherUserId}?userId={currentUserId}
        const response = await fetch(`/api/messages/${otherUserId}?userId=${currentUserId}`);
        const messages = await response.json();

        if (messages.length === 0) {
            messagesArea.innerHTML = '<div class="no-messages-placeholder" style="text-align:center;color:#94a3b8;font-size:13px;padding:20px;">No messages yet. Say hello! 👋</div>';
            return;
        }

        messages.forEach(msg => appendMessage(msg, false)); // false = no animation for history
        scrollToBottom();

    } catch (err) {
        console.error('Failed to load messages:', err);
    }
}

// =============================================
// 9. WEBSOCKET CONNECTION
// =============================================
function connectWebSocket() {
    connectionStatus.textContent = '● Connecting...';
    connectionStatus.className   = 'status-badge connecting';

    const socket = new SockJS('http://localhost:8080/ws');

    stompClient = new StompJs.Client({
        webSocketFactory: () => socket,

        onConnect: function (frame) {
            console.log('WebSocket connected');
            connectionStatus.textContent = '● Connected';
            connectionStatus.className   = 'status-badge online';

            // Subscribe to private messages
            stompClient.subscribe('/user/queue/messages', function (message) {
                const receivedMsg = JSON.parse(message.body);
                handleIncomingMessage(receivedMsg);
            });

            // Subscribe to online/offline status updates
            stompClient.subscribe('/topic/status', function (message) {
                const statusMsg = JSON.parse(message.body);
                console.log('Status update received:', statusMsg);
                updateUserStatus(statusMsg.userId, statusMsg.status);
            });

            // After connecting, re-fetch online users to catch anyone
            // who was already online before this page loaded
            fetch('http://localhost:8080/api/users/online')
                .then(res => res.json())
                .then(onlineIds => {
                    onlineUsers = new Set(onlineIds.map(id => String(id)));
                    console.log('Re-fetched online users after connect:', onlineUsers);
                    renderUserList(allUsers); // re-render with fresh online status
                });
        },

        onDisconnect: function () {
            connectionStatus.textContent = '● Disconnected';
            connectionStatus.className   = 'status-badge offline';
        },

        onStompError: function (frame) {
            console.error('STOMP error:', frame);
            connectionStatus.textContent = '● Error';
            connectionStatus.className   = 'status-badge offline';
        },

        connectHeaders: {
            userId: currentUserId
        },

        reconnectDelay: 5000
    });

    stompClient.activate();
}
// =============================================
// 10. HANDLE INCOMING WEBSOCKET MESSAGE
// =============================================
function handleIncomingMessage(msg) {
    // Only display messages that belong to the currently open conversation
    const isInCurrentChat =
        (String(msg.senderId) === String(selectedUser?.id) && String(msg.receiverId) === String(currentUserId)) ||
        (String(msg.senderId) === String(currentUserId)    && String(msg.receiverId) === String(selectedUser?.id));

    if (isInCurrentChat) {
        appendMessage(msg, true); // true = animate new message
        scrollToBottom();
         // If we received it (not sent by us), mark as read immediately
                if (String(msg.senderId) !== String(currentUserId)) {
                    clearUnreadBadge(msg.senderId);
                }
            } else {
                // Message from a different user — show unread badge
                if (String(msg.receiverId) === String(currentUserId)) {
                    const senderId = String(msg.senderId);
                    unreadCounts[senderId] = (unreadCounts[senderId] || 0) + 1;
                    updateUnreadBadge(senderId);
                }
    }
    // If message is from a different user (not currently open), you could show a notification here
}

// =============================================
// 11. SEND A MESSAGE
// =============================================
function sendMessage() {
    const text = messageInput.value.trim();

    if (!text) {
        messageInput.focus();
        return;
    }

    if (text.length > 1000) {
        alert('Message too long (max 1000 characters)');
        return;
    }

    if (!selectedUser) {
        alert('Please select a user to chat with');
        return;
    }

    if (!stompClient || !stompClient.connected) {
        alert('WebSocket not connected. Please wait...');
        return;
    }

    // Build message request object
    const messageRequest = {
        senderId:   parseInt(currentUserId),
        receiverId: parseInt(selectedUser.id),
        message:    text
    };

    // Send message to the server via STOMP
    // /app/chat.sendMessage → @MessageMapping("/chat.sendMessage") in Java
    stompClient.publish({
        destination: '/app/chat.sendMessage',
        body: JSON.stringify(messageRequest)
    });

    // Clear input
    messageInput.value   = '';
    charCount.textContent= '0/1000';
}

// =============================================
// 12. APPEND A MESSAGE TO THE CHAT AREA
// =============================================
function appendMessage(msg, animate) {
    const isSent = String(msg.senderId) === String(currentUserId);

    const wrapper = document.createElement('div');
    wrapper.className = `message-wrapper ${isSent ? 'sent' : 'received'}`;
    if (animate) wrapper.style.animation = 'fadeInUp 0.2s ease';

    const time = formatTime(msg.timestamp);

    wrapper.innerHTML = `
        <div class="message-bubble">${escapeHtml(msg.message)}</div>
        <div class="message-meta">${isSent ? 'You' : escapeHtml(msg.senderUsername)} · ${time}</div>
    `;

    // Remove the "no messages yet" placeholder if present
    const placeholder = messagesArea.querySelector('.no-messages-placeholder');
    if (placeholder) placeholder.remove();

    messagesArea.appendChild(wrapper);
}

// =============================================
// 13. UTILITY FUNCTIONS
// =============================================

// Scroll messages to the bottom
function scrollToBottom() {
    messagesArea.scrollTop = messagesArea.scrollHeight;
}

// Format ISO timestamp to readable time
function formatTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// Prevent XSS by escaping HTML characters
function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g,  '&amp;')
        .replace(/</g,  '&lt;')
        .replace(/>/g,  '&gt;')
        .replace(/"/g,  '&quot;')
        .replace(/'/g,  '&#039;');
}

// =============================================
// 14. EVENT LISTENERS
// =============================================

// Send message on button click
sendBtn.addEventListener('click', sendMessage);

// Send message on Enter key (Shift+Enter for new line)
messageInput.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

// Character counter
messageInput.addEventListener('input', function () {
    const len = messageInput.value.length;
    charCount.textContent = `${len}/1000`;
    charCount.style.color = len > 900 ? '#dc2626' : '#94a3b8';
});

// Search/filter users in sidebar
searchInput.addEventListener('input', function () {
    const query       = searchInput.value.toLowerCase();
    const filtered    = allUsers.filter(u =>
        u.username.toLowerCase().includes(query) ||
        u.email.toLowerCase().includes(query)
    );
    renderUserList(filtered);
});

function updateUserStatus(userId, status) {
    const isOnline = status === 'ONLINE';

    // Update our local set
    if (isOnline) {
        onlineUsers.add(String(userId));
    } else {
        onlineUsers.delete(String(userId));
    }

    // Find that user's list item and update the badge
    const li = userList.querySelector(`.user-item[data-user-id="${userId}"]`);
    if (li) {
        const badge = li.querySelector('.user-status');
        if (badge) {
            badge.className = `user-status ${isOnline ? 'status-online' : 'status-offline'}`;
            badge.textContent = isOnline ? '● Online' : '● Offline';
        }
    }

    // Also update the chat header if this user is currently open
    if (selectedUser && String(selectedUser.id) === String(userId)) {
        connectionStatus.textContent = isOnline ? '● Online' : '● Offline';
        connectionStatus.className   = `status-badge ${isOnline ? 'online' : 'offline'}`;
    }
}
// Logout button
logoutBtn.addEventListener('click', function () {
    if (!confirm('Are you sure you want to logout?')) return;

    // Disconnect WebSocket gracefully
    if (stompClient) {
        stompClient.deactivate();
    }

    // Clear localStorage session
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('email');

    // Redirect to login
    window.location.href = '/index.html';
});

// Add CSS animation for new messages
const style = document.createElement('style');
style.textContent = `
@keyframes fadeInUp {
    from { opacity: 0; transform: translateY(8px); }
    to   { opacity: 1; transform: translateY(0); }
}`;
document.head.appendChild(style);

// clear the unread messages
function clearUnreadBadge(senderId) {
    // Remove badge from UI immediately
    delete unreadCounts[senderId];
    const li    = userList.querySelector(`.user-item[data-user-id="${senderId}"]`);
    const badge = li?.querySelector('.unread-badge');
    if (badge) badge.remove();

    // Tell server to mark messages as read
    fetch(`http://localhost:8080/api/messages/read?receiverId=${currentUserId}&senderId=${senderId}`, {
        method: 'POST'
    }).catch(err => console.error('Failed to mark as read:', err));
}
function updateUnreadBadge(senderId) {
    const count = unreadCounts[senderId] || 0;
    const li    = userList.querySelector(`.user-item[data-user-id="${senderId}"]`);
    if (!li) return;

    // Remove existing badge
    const existingBadge = li.querySelector('.unread-badge');
    if (existingBadge) existingBadge.remove();

    // Add new badge if count > 0
    if (count > 0) {
        const badge       = document.createElement('div');
        badge.className   = 'unread-badge';
        badge.textContent = count > 99 ? '99+' : count;
        li.appendChild(badge);
    }
}
// =============================================
// 15. START
// =============================================
init();
