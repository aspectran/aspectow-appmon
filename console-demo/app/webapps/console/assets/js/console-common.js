/*
 * Copyright (c) 2026-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Formats a date value (ISO string, epochSecond object, timestamp number, or Date)
 * into the client's local time zone.
 *
 * @param {*} dateVal - The date value to format.
 * @param {string} [format='YYYY-MM-DD HH:mm:ss'] - The Day.js format string.
 * @returns {string} The formatted local date/time string, or '-' if invalid/empty.
 */
function formatDateTime(dateVal, format = 'YYYY-MM-DD HH:mm:ss') {
    if (!dateVal) return '-';
    if (typeof dayjs === 'undefined') return String(dateVal);

    let d;
    if (typeof dateVal === 'object' && dateVal !== null && dateVal.epochSecond !== undefined) {
        d = dayjs.unix(dateVal.epochSecond);
    } else if (typeof dateVal === 'number') {
        d = dayjs(dateVal);
    } else if (typeof dateVal === 'string') {
        d = dayjs.utc ? dayjs.utc(dateVal).local() : dayjs(dateVal);
    } else {
        d = dayjs(dateVal);
    }
    return d.isValid() ? d.format(format) : String(dateVal);
}

/**
 * Scans elements with the '.format-local-time' class within a container
 * and converts their 'data-utc' attribute value to the client's local time.
 *
 * @param {Element|jQuery|string} [container=document] - The root container to scan.
 */
function formatLocalTime(container) {
    if (typeof dayjs === 'undefined') return;

    if (typeof jQuery !== 'undefined') {
        const $targets = container ? $(container).find('.format-local-time') : $('.format-local-time');
        $targets.each(function() {
            const utc = $(this).data('utc');
            const fmt = $(this).data('format') || 'YYYY-MM-DD HH:mm:ss';
            if (utc) {
                const formatted = formatDateTime(utc, fmt);
                if (formatted !== '-') {
                    $(this).text(formatted);
                }
            }
        });
    } else {
        const root = (typeof container === 'string' ? document.querySelector(container) : container) || document;
        const targets = root.querySelectorAll('.format-local-time');
        targets.forEach(el => {
            const utc = el.getAttribute('data-utc');
            const fmt = el.getAttribute('data-format') || 'YYYY-MM-DD HH:mm:ss';
            if (utc) {
                const formatted = formatDateTime(utc, fmt);
                if (formatted !== '-') {
                    el.textContent = formatted;
                }
            }
        });
    }
}

// Automatically format local times once DOM is ready
if (typeof jQuery !== 'undefined') {
    $(function() {
        formatLocalTime();
    });
} else if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => formatLocalTime());
} else {
    formatLocalTime();
}
