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
 * APON (Aspectran Parameter Object Notation) Syntax Highlighter.
 * Parses raw APON text and returns HTML marked up with syntax classes.
 * 
 * @param {string} text - The raw APON format string.
 * @returns {string} The syntax-highlighted HTML string.
 *
 * @version 1.0
 * @last-modified 2026-06-24
 */
function highlightApon(text) {
    if (!text) return '';

    let i = 0;
    let result = '';
    const len = text.length;

    function escapeHtml(str) {
        return str
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
    }

    while (i < len) {
        const char = text[i];

        // 1. Comments: starting with # until end of line
        if (char === '#') {
            let comment = '';
            while (i < len && text[i] !== '\n') {
                comment += text[i];
                i++;
            }
            result += `<span class="apon-comment">${escapeHtml(comment)}</span>`;
            continue;
        }

        // 2. Multiline String: ''' ... '''
        if (text.startsWith("'''", i)) {
            let multilineStr = "'''";
            i += 3;
            while (i < len && !text.startsWith("'''", i)) {
                multilineStr += text[i];
                i++;
            }
            if (i < len) {
                multilineStr += "'''";
                i += 3;
            }
            result += `<span class="apon-string">${escapeHtml(multilineStr)}</span>`;
            continue;
        }

        // 3. String literal: " ... "
        if (char === '"') {
            let strLiteral = '"';
            i++;
            let escaped = false;
            while (i < len) {
                const c = text[i];
                strLiteral += c;
                i++;
                if (escaped) {
                    escaped = false;
                } else if (c === '\\') {
                    escaped = true;
                } else if (c === '"') {
                    break;
                }
            }
            result += `<span class="apon-string">${escapeHtml(strLiteral)}</span>`;
            continue;
        }

        // 4. Text block: ( ... )
        if (char === '(') {
            result += `<span class="apon-bracket">(</span>`;
            i++;
            let contentStart = i;
            
            // Find the true closing parenthesis for the text block
            while (i < len) {
                if (text[i] === ')') {
                    // Backtrack to check if this line starts with '|'
                    let isInsidePipeLine = false;
                    let k = i - 1;
                    while (k >= contentStart && text[k] !== '\n' && text[k] !== '\r') {
                        if (text[k] === '|') {
                            isInsidePipeLine = true;
                            break;
                        }
                        k--;
                    }
                    if (!isInsidePipeLine) {
                        // Found the closing parenthesis (no pipe character on this line)
                        break;
                    }
                }
                i++;
            }
            
            let content = text.substring(contentStart, i);
            result += `<span class="apon-string">${escapeHtml(content)}</span>`;
            if (i < len && text[i] === ')') {
                result += `<span class="apon-bracket">)</span>`;
                i++;
            }
            continue;
        }

        // 5. Brackets and Braces (curly and square)
        if (char === '{' || char === '}' || char === '[' || char === ']') {
            result += `<span class="apon-bracket">${char}</span>`;
            i++;
            continue;
        }

        // 6. White spaces
        if (char === ' ' || char === '\t' || char === '\n' || char === '\r') {
            result += char;
            i++;
            continue;
        }

        // 7. Words (Keys, Keywords, Values)
        let start = i;
        while (i < len && text[i] !== ' ' && text[i] !== '\t' && text[i] !== '\n' && text[i] !== '\r' && 
               text[i] !== '{' && text[i] !== '}' && text[i] !== '[' && text[i] !== ']' && 
               text[i] !== '(' && text[i] !== ')' &&
               text[i] !== ',' && text[i] !== ':' && text[i] !== '"' && text[i] !== '#') {
            i++;
        }

        if (start === i) {
            result += escapeHtml(text[i]);
            i++;
            continue;
        }

        let word = text.substring(start, i);

        if (i < len && text[i] === ':') {
            result += `<span class="apon-key">${escapeHtml(word)}</span>:`;
            i++; // consume ':'
            
            // Consume spaces after colon
            while (i < len && (text[i] === ' ' || text[i] === '\t')) {
                result += text[i];
                i++;
            }
            
            // If there's an unquoted value after colon, scan it as a block
            if (i < len && 
                text[i] !== '{' && text[i] !== '[' && text[i] !== '(' && 
                text[i] !== '"' && text[i] !== '\'' && text[i] !== '#' && 
                text[i] !== '\n' && text[i] !== '\r' && text[i] !== ',' &&
                text[i] !== '}' && text[i] !== ']') {
                
                let valStart = i;
                while (i < len && 
                       text[i] !== '\n' && text[i] !== '\r' && 
                       text[i] !== ',' && 
                       text[i] !== '}' && text[i] !== ']') {
                    
                    if (text[i] === '#' && (text[i-1] === ' ' || text[i-1] === '\t')) {
                        break;
                    }
                    i++;
                }
                
                let val = text.substring(valStart, i);
                let trimmedVal = val.trimEnd();
                let actualVal = val;
                if (trimmedVal.length < val.length) {
                    i = valStart + trimmedVal.length;
                    actualVal = trimmedVal;
                }
                
                if (actualVal === 'true' || actualVal === 'false' || actualVal === 'null') {
                    result += `<span class="apon-keyword">${actualVal}</span>`;
                } else if (!isNaN(actualVal) && !isNaN(parseFloat(actualVal))) {
                    result += `<span class="apon-number">${actualVal}</span>`;
                } else {
                    result += `<span class="apon-string">${escapeHtml(actualVal)}</span>`;
                }
            }
            continue;
        }

        // Unquoted value without a following colon (e.g. array element)
        if (word === 'true' || word === 'false' || word === 'null') {
            result += `<span class="apon-keyword">${word}</span>`;
        } else if (!isNaN(word) && !isNaN(parseFloat(word))) {
            result += `<span class="apon-number">${word}</span>`;
        } else {
            result += `<span class="apon-string">${escapeHtml(word)}</span>`;
        }
    }

    return result;
}
