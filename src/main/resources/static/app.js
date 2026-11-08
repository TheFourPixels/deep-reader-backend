const API_BASE = '/api';

const state = {
    books: [],
    currentBook: null,
    currentChapter: null,
    chapters: [],
    chapterAnnotations: [],
    allAnnotations: [],
    currentInsight: null,
    fontSize: 18,
    theme: localStorage.getItem('deep_reader_theme') || 'theme-paper'
};

const dom = {
    body: document.body,
    tabs: document.querySelectorAll('.nav-tab'),
    panes: document.querySelectorAll('.tab-pane'),
    backendStatus: document.getElementById('backend-status'),
    themeButtons: document.querySelectorAll('.theme-btn'),

    readerBookGenre: document.getElementById('reader-book-genre'),
    readerBookTitle: document.getElementById('reader-book-title'),
    readerBookAuthor: document.getElementById('reader-book-author'),
    chapterSelect: document.getElementById('chapter-select'),
    btnAddChapter: document.getElementById('btn-add-chapter'),

    currentChapterNum: document.getElementById('current-chapter-num'),
    currentChapterTitle: document.getElementById('current-chapter-title'),
    readerContent: document.getElementById('reader-content'),
    fontSizeDisplay: document.getElementById('font-size-display'),
    btnFontSmaller: document.getElementById('btn-font-smaller'),
    btnFontLarger: document.getElementById('btn-font-larger'),
    btnCreateNoteFromSelection: document.getElementById('btn-create-note-from-selection'),
    pageFooterStats: document.getElementById('page-footer-stats'),

    footnotePlaceholder: document.getElementById('footnote-placeholder'),
    footnoteActiveContent: document.getElementById('footnote-active-content'),
    insightBadge: document.getElementById('insight-badge'),
    insightPhrase: document.getElementById('insight-phrase'),
    insightQuote: document.getElementById('insight-quote'),
    insightExplanation: document.getElementById('insight-explanation'),
    btnCloseInsight: document.getElementById('btn-close-insight'),
    btnEditInsight: document.getElementById('btn-edit-insight'),
    btnDeleteInsight: document.getElementById('btn-delete-insight'),

    booksGrid: document.getElementById('books-grid'),
    bookSearch: document.getElementById('book-search'),
    btnSearchBooks: document.getElementById('btn-search-books'),
    btnAddBook: document.getElementById('btn-add-book'),
    modalBook: document.getElementById('modal-book'),
    formBook: document.getElementById('form-book'),
    modalBookTitle: document.getElementById('modal-book-title'),

    modalChapter: document.getElementById('modal-chapter'),
    formChapter: document.getElementById('form-chapter'),
    modalChapterTitle: document.getElementById('modal-chapter-title'),

    annotationsTableBody: document.getElementById('annotations-table-body'),
    filterAnnotationBook: document.getElementById('filter-annotation-book'),
    filterAnnotationType: document.getElementById('filter-annotation-type'),
    btnAddAnnotation: document.getElementById('btn-add-annotation'),
    modalAnnotation: document.getElementById('modal-annotation'),
    formAnnotation: document.getElementById('form-annotation'),
    modalAnnotationTitle: document.getElementById('modal-annotation-title'),
    annotationBookId: document.getElementById('annotation-book-id'),
    annotationChapterId: document.getElementById('annotation-chapter-id'),
    annotationTarget: document.getElementById('annotation-target'),
    annotationType: document.getElementById('annotation-type'),
    annotationExplanation: document.getElementById('annotation-explanation'),
    annotationSnippet: document.getElementById('annotation-snippet')
};

document.addEventListener('DOMContentLoaded', () => {
    initChaosWidget();
    applyTheme(state.theme);
    initEvents();
    checkHealth();
    loadBooks();
    loadAllAnnotations();
    setInterval(checkHealth, 20000);
});

async function checkHealth() {
    try {
        const res = await fetch(`${API_BASE}/health`);
        if (res.ok) {
            const data = await res.json();
            const dbStatus = data.database === 'CONNECTED' ? 'PostgreSQL подключен' : 'БД: ошибка';
            dom.backendStatus.className = 'status-indicator online';
            dom.backendStatus.innerHTML = `<span class="status-dot"></span><span class="status-label">${dbStatus}</span>`;
        } else {
            setOffline();
        }
    } catch {
        setOffline();
    }
}

function setOffline() {
    dom.backendStatus.className = 'status-indicator offline';
    dom.backendStatus.innerHTML = `<span class="status-dot"></span><span class="status-label">Сервер недоступен</span>`;
}

function applyTheme(themeName) {
    state.theme = themeName;
    dom.body.className = themeName;
    localStorage.setItem('deep_reader_theme', themeName);

    dom.themeButtons.forEach(btn => {
        btn.classList.toggle('active', btn.dataset.theme === themeName);
    });
}

function initEvents() {
    dom.tabs.forEach(tab => {
        tab.addEventListener('click', () => switchTab(tab.dataset.tab));
    });

    dom.themeButtons.forEach(btn => {
        btn.addEventListener('click', () => applyTheme(btn.dataset.theme));
    });

    dom.btnSearchBooks.addEventListener('click', () => loadBooks(dom.bookSearch.value));
    dom.bookSearch.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') loadBooks(dom.bookSearch.value);
    });
    dom.btnAddBook.addEventListener('click', () => openBookModal());
    dom.formBook.addEventListener('submit', handleBookSubmit);

    document.querySelectorAll('.modal-close, .modal-cancel').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const modal = e.target.closest('.modal');
            if (modal) modal.classList.remove('open');
        });
    });

    dom.chapterSelect.addEventListener('change', (e) => {
        const id = Number(e.target.value);
        if (id) selectChapter(id);
    });
    dom.btnAddChapter.addEventListener('click', () => {
        if (!state.currentBook) {
            alert('Сначала выберите книгу!');
            return;
        }
        openChapterModal();
    });
    dom.formChapter.addEventListener('submit', handleChapterSubmit);

    dom.btnFontSmaller.addEventListener('click', () => changeFontSize(-1));
    dom.btnFontLarger.addEventListener('click', () => changeFontSize(1));

    dom.btnCreateNoteFromSelection.addEventListener('click', handleCreateFromSelection);

    dom.btnCloseInsight.addEventListener('click', closeInsight);
    dom.btnEditInsight.addEventListener('click', () => {
        if (state.currentInsight) openAnnotationModal(state.currentInsight);
    });
    dom.btnDeleteInsight.addEventListener('click', async () => {
        if (state.currentInsight && confirm(`Удалить примечание для «${state.currentInsight.targetPhrase}»?`)) {
            await deleteAnnotation(state.currentInsight.id);
            closeInsight();
            if (state.currentChapter) {
                await loadChapterAnnotations(state.currentChapter.id);
                renderChapterText();
            }
        }
    });

    dom.btnAddAnnotation.addEventListener('click', () => openAnnotationModal());
    dom.formAnnotation.addEventListener('submit', handleAnnotationSubmit);
    dom.filterAnnotationBook.addEventListener('change', filterAnnotationsTable);
    dom.filterAnnotationType.addEventListener('change', filterAnnotationsTable);

    dom.annotationBookId.addEventListener('change', async (e) => {
        await populateChapterSelect(e.target.value, dom.annotationChapterId);
    });
}

function switchTab(tabId) {
    dom.tabs.forEach(t => t.classList.toggle('active', t.dataset.tab === tabId));
    dom.panes.forEach(p => p.classList.toggle('active', p.id === `tab-${tabId}`));
}

function changeFontSize(delta) {
    const nextSize = state.fontSize + delta;
    if (nextSize >= 14 && nextSize <= 28) {
        state.fontSize = nextSize;
        dom.readerContent.style.fontSize = `${nextSize}px`;
        dom.fontSizeDisplay.textContent = `${nextSize}px`;
    }
}

async function loadBooks(query = '') {
    try {
        const url = query ? `${API_BASE}/books?search=${encodeURIComponent(query)}` : `${API_BASE}/books`;
        const res = await fetch(url);
        if (!res.ok) throw new Error('Ошибка загрузки книг');
        state.books = await res.json();
        renderBooksGrid();
        updateBookDropdowns();

        if (state.books.length > 0 && !state.currentBook) {
            selectBookForReader(state.books[0]);
        }
    } catch (err) {
        console.error(err);
        dom.booksGrid.innerHTML = `<div class="empty-state">Не удалось загрузить каталог книг: ${err.message}</div>`;
    }
}

function renderBooksGrid() {
    if (!state.books.length) {
        dom.booksGrid.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 3rem; color: var(--text-secondary);">
                <h3>Каталог пуст</h3>
                <p>Добавьте свою первую книгу с помощью кнопки выше.</p>
            </div>`;
        return;
    }

    dom.booksGrid.innerHTML = state.books.map(book => `
        <div class="book-card" data-id="${book.id}">
            <div class="book-card-cover">
                ${book.coverUrl ? `<img src="${escapeHtml(book.coverUrl)}" alt="${escapeHtml(book.title)}">` : `<span class="book-cover-fallback">📖</span>`}
            </div>
            <div class="book-card-body">
                <h3 class="book-card-title">${escapeHtml(book.title)}</h3>
                <p class="book-card-author">${escapeHtml(book.author)}</p>
                <p class="book-card-desc">${escapeHtml(book.description || 'Без описания')}</p>
                <div class="book-card-meta">
                    <span class="badge-meta">${escapeHtml(book.genre || 'Классика')}</span>
                    <span class="badge-meta">${escapeHtml(book.language.toUpperCase())}</span>
                    <span class="badge-meta">${book.chaptersCount} глав</span>
                    <span class="badge-meta">📌 ${book.annotationsCount} сносок</span>
                </div>
                <div class="book-card-footer">
                    <button class="btn btn-primary btn-sm" onclick="openReaderForBook(${book.id})">Читать</button>
                    <button class="btn btn-secondary btn-sm" onclick="editBook(${book.id})">Изменить</button>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteBook(${book.id})">Удалить</button>
                </div>
            </div>
        </div>
    `).join('');
}

function openBookModal(book = null) {
    document.getElementById('book-form-id').value = book ? book.id : '';
    document.getElementById('book-title').value = book ? book.title : '';
    document.getElementById('book-author').value = book ? book.author : '';
    document.getElementById('book-genre').value = book ? book.genre || '' : '';
    document.getElementById('book-language').value = book ? book.language : 'en';
    document.getElementById('book-cover').value = book ? book.coverUrl || '' : '';
    document.getElementById('book-desc').value = book ? book.description || '' : '';
    dom.modalBookTitle.textContent = book ? 'Редактировать книгу' : 'Добавить книгу в библиотеку';
    dom.modalBook.classList.add('open');
}

async function handleBookSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('book-form-id').value;
    const payload = {
        title: document.getElementById('book-title').value,
        author: document.getElementById('book-author').value,
        genre: document.getElementById('book-genre').value,
        language: document.getElementById('book-language').value,
        coverUrl: document.getElementById('book-cover').value,
        description: document.getElementById('book-desc').value
    };

    try {
        const method = id ? 'PUT' : 'POST';
        const url = id ? `${API_BASE}/books/${id}` : `${API_BASE}/books`;
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('Ошибка при сохранении книги');
        dom.modalBook.classList.remove('open');
        await loadBooks();
    } catch (err) {
        alert(err.message);
    }
}

window.editBook = function(id) {
    const book = state.books.find(b => b.id === id);
    if (book) openBookModal(book);
};

window.deleteBook = async function(id) {
    if (!confirm('Вы действительно хотите удалить эту книгу со всеми главами и сносками?')) return;
    try {
        const res = await fetch(`${API_BASE}/books/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Не удалось удалить книгу');
        await loadBooks();
        await loadAllAnnotations();
    } catch (err) {
        alert(err.message);
    }
};

window.openReaderForBook = async function(id) {
    const book = state.books.find(b => b.id === id);
    if (book) {
        await selectBookForReader(book);
        switchTab('reader');
    }
};

async function selectBookForReader(book) {
    state.currentBook = book;
    dom.readerBookTitle.textContent = book.title;
    dom.readerBookAuthor.textContent = book.author;
    dom.readerBookGenre.textContent = book.genre || 'Классика';

    try {
        const res = await fetch(`${API_BASE}/books/${book.id}/chapters`);
        if (!res.ok) throw new Error('Ошибка загрузки глав');
        state.chapters = await res.json();

        dom.chapterSelect.innerHTML = state.chapters.map(ch => `
            <option value="${ch.id}">Глава ${ch.chapterNumber}: ${escapeHtml(ch.title)}</option>
        `).join('');

        if (state.chapters.length > 0) {
            selectChapter(state.chapters[0].id);
        } else {
            dom.chapterSelect.innerHTML = '<option value="">(Глав пока нет)</option>';
            dom.currentChapterNum.textContent = '';
            dom.currentChapterTitle.textContent = 'В книге пока нет глав';
            dom.readerContent.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 3rem;">Нажмите <b>+ Добавить главу</b> слева, чтобы вставить текст произведения.</div>`;
            dom.pageFooterStats.textContent = '';
        }
    } catch (err) {
        console.error(err);
    }
}

async function selectChapter(chapterId) {
    const ch = state.chapters.find(c => c.id === chapterId);
    if (!ch) return;
    state.currentChapter = ch;
    dom.chapterSelect.value = ch.id;
    dom.currentChapterNum.textContent = `Глава ${ch.chapterNumber}`;
    dom.currentChapterTitle.textContent = ch.title;

    await loadChapterAnnotations(chapterId);
    renderChapterText();
}

async function loadChapterAnnotations(chapterId) {
    try {
        const res = await fetch(`${API_BASE}/chapters/${chapterId}/annotations`);
        state.chapterAnnotations = res.ok ? await res.json() : [];
    } catch {
        state.chapterAnnotations = [];
    }
}

function renderChapterText() {
    if (!state.currentChapter) return;
    const text = state.currentChapter.content;
    const words = text.trim().split(/\s+/).length;
    const annCount = state.chapterAnnotations ? state.chapterAnnotations.length : 0;
    dom.pageFooterStats.textContent = `Слов: ${words} • Сносок к главе: ${annCount}`;

    if (!state.chapterAnnotations || state.chapterAnnotations.length === 0) {
        dom.readerContent.textContent = text;
        return;
    }

    const annotations = [...state.chapterAnnotations].filter(a => a.targetPhrase && a.targetPhrase.trim());
    annotations.sort((a, b) => b.targetPhrase.length - a.targetPhrase.length);

    const matches = [];
    const lowerText = text.toLowerCase();
    const occupied = new Uint8Array(text.length);

    annotations.forEach(ann => {
        const phrase = ann.targetPhrase.trim().toLowerCase();
        if (!phrase) return;
        let pos = lowerText.indexOf(phrase);
        while (pos !== -1) {
            let overlaps = false;
            for (let i = pos; i < pos + phrase.length; i++) {
                if (occupied[i]) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                for (let i = pos; i < pos + phrase.length; i++) {
                    occupied[i] = 1;
                }
                matches.push({
                    start: pos,
                    end: pos + phrase.length,
                    ann: ann
                });
            }
            pos = lowerText.indexOf(phrase, pos + 1);
        }
    });

    matches.sort((a, b) => a.start - b.start);

    let resultHtml = '';
    let lastIndex = 0;
    matches.forEach(m => {
        if (m.start > lastIndex) {
            resultHtml += escapeHtml(text.substring(lastIndex, m.start));
        }
        const phraseText = escapeHtml(text.substring(m.start, m.end));
        const typeClass = escapeHtml(m.ann.type.toLowerCase());
        const typeTitle = escapeHtml(m.ann.typeDisplayName || m.ann.type);
        resultHtml += `<span class="footnote-mark ${typeClass}" data-id="${m.ann.id}" title="Сноска: ${typeTitle}">${phraseText}</span>`;
        lastIndex = m.end;
    });
    if (lastIndex < text.length) {
        resultHtml += escapeHtml(text.substring(lastIndex));
    }

    dom.readerContent.innerHTML = resultHtml;

    dom.readerContent.querySelectorAll('.footnote-mark').forEach(el => {
        el.addEventListener('click', () => {
            const id = Number(el.dataset.id);
            const ann = state.chapterAnnotations.find(a => a.id === id);
            if (ann) openInsight(ann);
        });
    });
}

function openInsight(ann) {
    state.currentInsight = ann;
    dom.insightBadge.textContent = ann.typeDisplayName || ann.type;
    dom.insightPhrase.textContent = ann.targetPhrase;
    dom.insightQuote.textContent = ann.contextSnippet ? `«${ann.contextSnippet}»` : '';
    dom.insightQuote.style.display = ann.contextSnippet ? 'block' : 'none';
    dom.insightExplanation.textContent = ann.explanation;

    dom.footnotePlaceholder.style.display = 'none';
    dom.footnoteActiveContent.style.display = 'block';
}

function closeInsight() {
    state.currentInsight = null;
    dom.footnoteActiveContent.style.display = 'none';
    dom.footnotePlaceholder.style.display = 'block';
}

function handleCreateFromSelection() {
    const sel = window.getSelection().toString().trim();
    if (!sel) {
        alert('Выделите фразу, сленг или метафору в тексте книги курсором!');
        return;
    }
    openAnnotationModal({
        targetPhrase: sel,
        bookId: state.currentBook ? state.currentBook.id : null,
        chapterId: state.currentChapter ? state.currentChapter.id : null,
        contextSnippet: sel
    });
}

function openChapterModal(chapter = null) {
    document.getElementById('chapter-form-id').value = chapter ? chapter.id : '';
    document.getElementById('chapter-number').value = chapter ? chapter.chapterNumber : (state.chapters.length + 1);
    document.getElementById('chapter-title').value = chapter ? chapter.title : '';
    document.getElementById('chapter-content').value = chapter ? chapter.content : '';
    dom.modalChapterTitle.textContent = chapter ? 'Редактировать главу' : 'Добавить главу книги';
    dom.modalChapter.classList.add('open');
}

async function handleChapterSubmit(e) {
    e.preventDefault();
    if (!state.currentBook) return;
    const id = document.getElementById('chapter-form-id').value;
    const payload = {
        chapterNumber: Number(document.getElementById('chapter-number').value),
        title: document.getElementById('chapter-title').value,
        content: document.getElementById('chapter-content').value
    };

    try {
        const method = id ? 'PUT' : 'POST';
        const url = id ? `${API_BASE}/chapters/${id}` : `${API_BASE}/books/${state.currentBook.id}/chapters`;
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('Ошибка сохранения главы');
        dom.modalChapter.classList.remove('open');
        await selectBookForReader(state.currentBook);
    } catch (err) {
        alert(err.message);
    }
}

async function loadAllAnnotations() {
    try {
        if (!state.currentBook && state.books.length > 0) {
            state.currentBook = state.books[0];
        }
        if (!state.currentBook) return;
        const res = await fetch(`${API_BASE}/books/${state.currentBook.id}/annotations`);
        if (!res.ok) throw new Error('Ошибка загрузки сносок');
        state.allAnnotations = await res.json();
        renderAnnotationsTable(state.allAnnotations);
    } catch (err) {
        console.error(err);
    }
}

function renderAnnotationsTable(list) {
    if (!list.length) {
        dom.annotationsTableBody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 2rem;">Сносок пока нет.</td></tr>`;
        return;
    }

    dom.annotationsTableBody.innerHTML = list.map(ann => `
        <tr>
            <td><b style="font-family: 'Lora', serif; font-size: 1rem;">${escapeHtml(ann.targetPhrase)}</b></td>
            <td><span class="badge-type">${escapeHtml(ann.typeDisplayName || ann.type)}</span></td>
            <td style="line-height: 1.5;">${escapeHtml(ann.explanation)}</td>
            <td style="font-size: 0.82rem; font-style: italic; color: var(--text-secondary);">${escapeHtml(ann.contextSnippet || '—')}</td>
            <td style="text-align: right; white-space: nowrap;">
                <button class="btn btn-secondary btn-sm" onclick="editAnnotation(${ann.id})">Ред.</button>
                <button class="btn btn-outline-danger btn-sm" onclick="deleteAnnotationRow(${ann.id})">Удалить</button>
            </td>
        </tr>
    `).join('');
}

async function filterAnnotationsTable() {
    const bookId = dom.filterAnnotationBook.value;
    const type = dom.filterAnnotationType.value;

    if (bookId) {
        try {
            const res = await fetch(`${API_BASE}/books/${bookId}/annotations`);
            if (res.ok) {
                state.allAnnotations = await res.json();
            }
        } catch (e) {
            console.error(e);
        }
    }

    let filtered = state.allAnnotations;
    if (bookId) filtered = filtered.filter(a => a.bookId === Number(bookId));
    if (type) filtered = filtered.filter(a => a.type === type);

    renderAnnotationsTable(filtered);
}

function updateBookDropdowns() {
    dom.filterAnnotationBook.innerHTML = '<option value="">Все книги</option>' +
        state.books.map(b => `<option value="${b.id}">${escapeHtml(b.title)}</option>`).join('');

    dom.annotationBookId.innerHTML = state.books.map(b => `<option value="${b.id}">${escapeHtml(b.title)}</option>`).join('');
}

async function populateChapterSelect(bookId, selectEl, selectedChapterId = null) {
    if (!bookId) {
        selectEl.innerHTML = '<option value="">(Общая для книги)</option>';
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/books/${bookId}/chapters`);
        if (res.ok) {
            const chs = await res.json();
            selectEl.innerHTML = '<option value="">(Общая для книги)</option>' +
                chs.map(c => `<option value="${c.id}" ${c.id === selectedChapterId ? 'selected' : ''}>Глава ${c.chapterNumber}: ${escapeHtml(c.title)}</option>`).join('');
        }
    } catch (e) {
        console.error(e);
    }
}

async function openAnnotationModal(ann = null) {
    updateBookDropdowns();
    document.getElementById('annotation-form-id').value = ann && ann.id ? ann.id : '';
    dom.annotationTarget.value = ann ? ann.targetPhrase || '' : '';
    dom.annotationType.value = ann ? ann.type || 'SLANG' : 'SLANG';
    dom.annotationExplanation.value = ann ? ann.explanation || '' : '';
    dom.annotationSnippet.value = ann ? ann.contextSnippet || '' : '';

    const selectedBookId = ann && ann.bookId ? ann.bookId : (state.currentBook ? state.currentBook.id : (state.books[0] ? state.books[0].id : ''));
    dom.annotationBookId.value = selectedBookId;

    await populateChapterSelect(selectedBookId, dom.annotationChapterId, ann ? ann.chapterId : null);

    dom.modalAnnotationTitle.textContent = ann && ann.id ? 'Редактировать сноску' : 'Добавить сноску литературоведа';
    dom.modalAnnotation.classList.add('open');
}

async function handleAnnotationSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('annotation-form-id').value;
    const bookId = Number(dom.annotationBookId.value);
    
    const payload = {
        chapterId: dom.annotationChapterId.value ? Number(dom.annotationChapterId.value) : null,
        targetPhrase: dom.annotationTarget.value,
        type: dom.annotationType.value,
        explanation: dom.annotationExplanation.value,
        contextSnippet: dom.annotationSnippet.value
    };

    try {
        const method = id ? 'PUT' : 'POST';

        const url = id ? `${API_BASE}/annotations/${id}` : `${API_BASE}/books/${bookId}/annotations`;

        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('Ошибка сохранения сноски');
        dom.modalAnnotation.classList.remove('open');
        await loadAllAnnotations();
        if (state.currentChapter) {
            await loadChapterAnnotations(state.currentChapter.id);
            renderChapterText();
        }
        await loadBooks();
    } catch (err) {
        alert(err.message);
    }
}

window.editAnnotation = function(id) {
    const ann = state.allAnnotations.find(a => a.id === id);
    if (ann) openAnnotationModal(ann);
};

window.deleteAnnotationRow = async function(id) {
    if (!confirm('Удалить эту сноску?')) return;
    await deleteAnnotation(id);
    await loadAllAnnotations();
    if (state.currentChapter) {
        await loadChapterAnnotations(state.currentChapter.id);
        renderChapterText();
    }
    await loadBooks();
};

async function deleteAnnotation(id) {
    try {
        const res = await fetch(`${API_BASE}/annotations/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Ошибка при удалении');
    } catch (err) {
        alert(err.message);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}


async function checkChaosStatus() {
    try {
        const res = await fetch(`${API_BASE}/chaos/status`);
        if (res.ok) {
            const data = await res.json();
            const badge = document.getElementById('chaos-badge');
            const btnFault = document.getElementById('btn-toggle-chaos');
            const btnReset = document.getElementById('btn-reset-chaos');
            if (badge && btnFault && btnReset) {
                if (data.enabled) {
                    badge.className = 'badge-status badge-chaos';
                    badge.textContent = `Сбой активен (${Math.round(data.errorRate * 100)}% 500, +${data.delayMs}ms)`;
                    btnFault.style.display = 'none';
                    btnReset.style.display = 'inline-block';
                } else {
                    badge.className = 'badge-status badge-normal';
                    badge.textContent = 'Норма';
                    btnFault.style.display = 'inline-block';
                    btnReset.style.display = 'none';
                }
            }
        }
    } catch (e) {
    }
}

async function triggerChaos() {
    try {
        const res = await fetch(`${API_BASE}/chaos/fault?errorRate=0.4&delayMs=500`, { method: 'POST' });
        if (res.ok) {
            await checkChaosStatus();
        }
    } catch (e) {
        alert('Ошибка переключения сбоя: ' + e.message);
    }
}

async function resetChaos() {
    try {
        const res = await fetch(`${API_BASE}/chaos/reset`, { method: 'POST' });
        if (res.ok) {
            await checkChaosStatus();
        }
    } catch (e) {
        alert('Ошибка сброса сбоя: ' + e.message);
    }
}

function initChaosWidget() {
    const btnFault = document.getElementById('btn-toggle-chaos');
    const btnReset = document.getElementById('btn-reset-chaos');
    if (btnFault) btnFault.addEventListener('click', triggerChaos);
    if (btnReset) btnReset.addEventListener('click', resetChaos);
    checkChaosStatus();
    setInterval(checkChaosStatus, 4000);
}
