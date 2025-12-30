const API_base = '/api';
let currentPage = 1;
let currentTicketId = null;
let currentDetailId = null;
let detailInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    const isLoginPage = window.location.pathname.endsWith('login.html');
    const isPanelPage = window.location.pathname.endsWith('panel.html') || window.location.pathname === '/' || window.location.pathname.endsWith('/');

    if (isLoginPage) {
        checkAuthOnLogin();
    } else {
        initPanel();
    }
});

async function checkAuthOnLogin() {
    try {
        const response = await fetch(`${API_base}/auth/me`);
        if (response.ok) {
            window.location.href = '/panel.html';
        }
    } catch (e) {
    }

    const loginBtn = document.getElementById('login-btn');
    if (loginBtn) {
        loginBtn.addEventListener('click', (e) => {
            e.preventDefault();
            window.location.href = `${API_base}/auth/discord`;
        });
    }
}

async function initPanel() {
    try {
        const response = await fetch(`${API_base}/auth/me`);
        if (!response.ok) {
            throw new Error('Nao autenticado');
        }
        const user = await response.json();
        updateUserInfo(user);
    } catch (error) {
        console.error('Auth error:', error);
        window.location.href = '/login.html';
        return;
    }

    setupEventListeners();

    loadTickets();
    loadStats();

    setInterval(() => {
        loadTickets();
        loadStats();
    }, 30000);
}

function updateUserInfo(user) {
    const nameEl = document.getElementById('user-name');
    const avatarEl = document.getElementById('user-avatar');

    if (nameEl) nameEl.textContent = user.username;
    if (avatarEl) {
        avatarEl.src = user.avatar || 'https://cdn.discordapp.com/embed/avatars/0.png';
    }
}

function setupEventListeners() {
    document.getElementById('logout-btn')?.addEventListener('click', async () => {
        await fetch(`${API_base}/auth/logout`);
        window.location.href = '/login.html';
    });

    document.getElementById('btn-refresh')?.addEventListener('click', loadTickets);

    document.getElementById('close-modal')?.addEventListener('click', closeModal);
    document.getElementById('send-reply')?.addEventListener('click', sendReply);
    document.getElementById('close-ticket')?.addEventListener('click', closeTicket);

    document.getElementById('prev-page')?.addEventListener('click', () => changePage(-1));
    document.getElementById('next-page')?.addEventListener('click', () => changePage(1));

    const modal = document.getElementById('ticket-modal');
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal();
        });
    }
}

async function loadStats() {
    try {
        const response = await fetch(`${API_base}/stats`);
        if (!response.ok) return;

        const data = await response.json();

        const totalEl = document.getElementById('total-tickets');
        const resolvedEl = document.getElementById('stat-resolved');
        const avgEl = document.getElementById('stat-avg-time');

        if (totalEl) totalEl.textContent = data.openTickets;
        if (resolvedEl) resolvedEl.textContent = data.resolvedTickets;
        if (avgEl) avgEl.textContent = data.avgTimeFormatted || '--';

    } catch (e) {
        console.error('Erro ao carregar stats', e);
    }
}

async function loadTickets() {
    const container = document.getElementById('tickets-container');
    if (!container) return;

    if (!container.children.length || container.innerHTML.includes('Carregando')) {
        container.innerHTML = '<div class="ticket-item" style="justify-content: center;"><i class="fas fa-spinner fa-spin"></i> Carregando...</div>';
    }

    try {
        const response = await fetch(`${API_base}/tickets?page=${currentPage}&perPage=10`);
        if (!response.ok) {
            if (response.status === 401) window.location.href = '/login.html';
            throw new Error('Falha ao carregar tickets');
        }

        const data = await response.json();

        const totalEl = document.getElementById('total-tickets');
        if (totalEl) totalEl.textContent = data.total;

        renderTickets(data.tickets);
        updatePagination(data.page, Math.ceil(data.total / data.perPage));

    } catch (error) {
        console.error(error);
        container.innerHTML = '<div class="ticket-item" style="color: red; justify-content: center;">Erro ao carregar tickets. Tente recarregar.</div>';
    }
}

function renderTickets(tickets) {
    const container = document.getElementById('tickets-container');
    container.innerHTML = '';

    if (!tickets || tickets.length === 0) {
        container.innerHTML = '<div class="ticket-item" style="justify-content: center;">Nenhum ticket encontrado.</div>';
        return;
    }

    tickets.forEach(ticket => {
        const date = new Date(ticket.createdAt).toLocaleString('pt-BR');

        let statusLabel = ticket.status;
        if (statusLabel === 'OPEN') statusLabel = 'Aberto';
        if (statusLabel === 'IN_PROGRESS') statusLabel = 'Em Progresso';
        if (statusLabel === 'CLOSED') statusLabel = 'Fechado';

        const el = document.createElement('div');
        el.className = 'ticket-item';
        el.onclick = () => openTicket(ticket.id);

        el.innerHTML = `
            <div class="ticket-main">
                <div class="ticket-status status-${ticket.status}"></div>
                <div class="ticket-content">
                    <h4>#${ticket.id} - ${escapeHtml(ticket.playerName)}</h4>
                    <div class="ticket-meta">
                        <span><i class="far fa-clock"></i> ${date}</span>
                        <span><i class="far fa-comment"></i> ${ticket.replies ? ticket.replies.length : 0} respostas</span>
                    </div>
                </div>
            </div>
            <div class="ticket-status-badge badge-${ticket.status.toLowerCase()}">
                ${statusLabel}
            </div>
        `;

        container.appendChild(el);
    });
}

async function openTicket(id) {
    currentTicketId = id;
    currentDetailId = id;
    const modal = document.getElementById('ticket-modal');
    const messagesContainer = document.getElementById('ticket-messages');

    document.getElementById('modal-title').textContent = `Carregando Ticket #${id}...`;
    messagesContainer.innerHTML = '<div style="text-align: center; padding: 20px;"><i class="fas fa-spinner fa-spin"></i></div>';
    document.getElementById('reply-input').value = '';

    modal.classList.add('active');

    try {
        const response = await fetch(`${API_base}/tickets/${id}`);
        if (!response.ok) throw new Error('Erro ao abrir ticket');

        const ticket = await response.json();
        updateTicketStatusUI(ticket);

        document.getElementById('modal-title').textContent = `Ticket #${ticket.id} - ${escapeHtml(ticket.playerName)}`;

        let html = '';

        html += renderMessage({
            author: ticket.playerName,
            authorType: 'PLAYER',
            message: ticket.message,
            createdAt: ticket.createdAt
        });

        if (ticket.replies) {
            ticket.replies.forEach(reply => {
                html += renderMessage(reply);
            });
        }

        if (messagesContainer) messagesContainer.innerHTML = html;
        scrollToBottom();

        if (detailInterval) clearInterval(detailInterval);
        detailInterval = setInterval(() => refreshTicketMessages(id), 2000);

    } catch (error) {
        console.error(error);
        if (messagesContainer) messagesContainer.innerHTML = '<div style="color: red; text-align: center;">Erro ao carregar detalhes.</div>';
    }
}

async function refreshTicketMessages(id) {
    if (!currentDetailId || currentDetailId !== id) return;

    try {
        const response = await fetch(`${API_base}/tickets/${id}`);
        if (response.ok) {
            const ticket = await response.json();
            updateTicketStatusUI(ticket);
            const messagesContainer = document.getElementById('ticket-messages');

            let html = '';
            html += renderMessage({
                author: ticket.playerName,
                authorType: 'PLAYER',
                message: ticket.message,
                createdAt: ticket.createdAt
            });
            if (ticket.replies) {
                ticket.replies.forEach(reply => {
                    html += renderMessage(reply);
                });
            }
            if (messagesContainer) messagesContainer.innerHTML = html;
            scrollToBottom();

            const modalStatusEl = document.querySelector('#ticket-modal .ticket-status-badge');
            if (modalStatusEl) {
                let statusLabel = ticket.status;
                if (statusLabel === 'OPEN') statusLabel = 'Aberto';
                if (statusLabel === 'IN_PROGRESS') statusLabel = 'Em Progresso';
                if (statusLabel === 'CLOSED') statusLabel = 'Fechado';
                modalStatusEl.textContent = statusLabel;
                modalStatusEl.className = `ticket-status-badge badge-${ticket.status.toLowerCase()}`;
            }
        }
    } catch (e) {
        console.error("Erro no polling do chat:", e);
    }
}

function renderMessage(msg) {
    const isStaff = msg.authorType === 'STAFF';
    const badgeClass = isStaff ? 'badge-staff' : 'badge-player';
    const roleName = isStaff ? 'STAFF' : 'JOGADOR';
    const date = new Date(msg.createdAt).toLocaleString('pt-BR');

    return `
        <div class="message-bubble ${isStaff ? 'staff-msg' : ''}">
            <div class="message-header">
                <div>
                    <span class="author">${escapeHtml(msg.author)}</span>
                    <span class="badge ${badgeClass}">${roleName}</span>
                </div>
                <span style="font-size: 12px; color: #9CA3AF;">${date}</span>
            </div>
            <div class="message-content" style="white-space: pre-wrap; line-height: 1.5;">${escapeHtml(msg.message)}</div>
        </div>
    `;
}

function closeModal() {
    document.getElementById('ticket-modal').classList.remove('active');
    currentTicketId = null;
    currentDetailId = null;
    if (detailInterval) {
        clearInterval(detailInterval);
        detailInterval = null;
    }
}

async function sendReply() {
    if (!currentDetailId) return;

    const input = document.getElementById('reply-input');
    const message = input.value.trim();

    if (!message) return;

    const btn = document.getElementById('send-reply');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';
    btn.disabled = true;

    try {
        const response = await fetch(`${API_base}/tickets/${currentDetailId}/reply`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json; charset=UTF-8' },
            body: JSON.stringify({ message })
        });

        if (!response.ok) throw new Error('Erro ao responder');

        input.value = '';
        input.focus();
        openTicket(currentTicketId);
        loadTickets();

    } catch (error) {
        alert('Falha ao enviar resposta: ' + error.message);
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

async function closeTicket() {
    if (!currentTicketId || !confirm('Tem certeza que deseja fechar este ticket?')) return;

    try {
        const response = await fetch(`${API_base}/tickets/${currentTicketId}/close`, {
            method: 'POST'
        });

        if (!response.ok) throw new Error('Erro ao fechar');

        closeModal();
        loadTickets();

    } catch (error) {
        alert('Erro ao fechar ticket');
    }
}

function changePage(delta) {
    currentPage += delta;
    if (currentPage < 1) currentPage = 1;
    loadTickets();
}

function updatePagination(page, totalPages) {
    const info = document.getElementById('page-info');
    const prev = document.getElementById('prev-page');
    const next = document.getElementById('next-page');

    if (info) info.textContent = `Página ${page} de ${totalPages || 1}`;
    if (prev) prev.disabled = page <= 1;
    if (next) next.disabled = page >= totalPages;
}

function scrollToBottom() {
    const container = document.getElementById('ticket-messages');
    if (container) container.scrollTop = container.scrollHeight;
}

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function updateTicketStatusUI(ticket) {
    const modalFooter = document.querySelector('.modal-footer');
    if (!modalFooter) return;

    const replyArea = modalFooter.querySelector('.reply-area');
    let alert = document.getElementById('closed-alert');

    const isClosed = ticket.status === 'CLOSED' || ticket.status === 'RESOLVED';

    if (isClosed) {
        if (replyArea) replyArea.style.display = 'none';

        if (!alert) {
            alert = document.createElement('div');
            alert.id = 'closed-alert';
            alert.style.cssText = 'background: #FEE2E2; color: #DC2626; padding: 15px; border-radius: 8px; text-align: center; font-weight: 500; display: flex; align-items: center; justify-content: center; width: 100%;';
            alert.innerHTML = '<i class="fas fa-lock" style="margin-right: 8px;"></i> Este ticket foi encerrado.';
            modalFooter.insertBefore(alert, modalFooter.firstChild);
        }
    } else {
        if (replyArea) replyArea.style.display = 'block';
        if (alert) alert.remove();
    }
}
