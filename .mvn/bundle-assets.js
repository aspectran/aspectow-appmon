#!/usr/bin/env node
/*
 * Copyright (c) 2020-present The Aspectran Project
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

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const rootDir = path.resolve(__dirname, "..");
const assetsDir = path.join(rootDir, "assets");

const licenseHeader = `/*
 * Copyright (c) 2020-present The Aspectran Project
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
`;

/**
 * Finds the latest versioned directory for the given asset prefix (e.g., 'appmon', 'console').
 * Supports passing a specific version via CLI args (e.g. node bundle-assets.js 4.1)
 */
function getAssetDir(prefix, targetVersion) {
    if (targetVersion) {
        const explicitDir = path.join(assetsDir, `${prefix}@${targetVersion}`);
        if (fs.existsSync(explicitDir)) return explicitDir;
    }

    const dirs = fs.readdirSync(assetsDir).filter((name) => {
        return name.startsWith(prefix + "@") && fs.statSync(path.join(assetsDir, name)).isDirectory();
    });

    if (dirs.length === 0) {
        throw new Error(`[bundle-assets] No asset directory found matching ${prefix}@ in ${assetsDir}`);
    }

    dirs.sort((a, b) => {
        const verA = a.split("@")[1].split(".").map(Number);
        const verB = b.split("@")[1].split(".").map(Number);
        for (let i = 0; i < Math.max(verA.length, verB.length); i++) {
            const numA = verA[i] || 0;
            const numB = verB[i] || 0;
            if (numA !== numB) return numA - numB;
        }
        return 0;
    });

    const latest = dirs[dirs.length - 1];
    return path.join(assetsDir, latest);
}

function bundleJsFiles(srcDir, fileList, outputBundleName, destDirs) {
    console.log(`[bundle-assets] Bundling JS (${srcDir}) -> ${outputBundleName}.js ...`);
    let bundleContent = licenseHeader + "\n";
    fileList.forEach((file) => {
        const filePath = path.join(srcDir, file);
        if (!fs.existsSync(filePath)) {
            console.error(`[bundle-assets] Error: File not found: ${filePath}`);
            process.exit(1);
        }
        let content = fs.readFileSync(filePath, "utf-8");
        content = content.replace(/^\/\*[\s\S]*?\*\/\r?\n*/, "").trim();
        bundleContent += content + "\n\n";
    });

    const bundleJs = bundleContent.trim() + "\n";
    const bundleFile = path.join(srcDir, `${outputBundleName}.js`);
    fs.writeFileSync(bundleFile, bundleJs, "utf-8");
    console.log(`[bundle-assets] Created ${outputBundleName}.js (${(fs.statSync(bundleFile).size / 1024).toFixed(1)} KB)`);

    // Minify with terser
    const minFile = path.join(srcDir, `${outputBundleName}.min.js`);
    try {
        console.log(`[bundle-assets] Minifying ${outputBundleName}.js with terser...`);
        execSync(`npx --yes terser "${bundleFile}" -o "${minFile}" --comments "/^!/"`, {
            stdio: "inherit"
        });
        const minContent = fs.readFileSync(minFile, "utf-8");
        if (!minContent.startsWith("/*")) {
            fs.writeFileSync(minFile, licenseHeader + minContent, "utf-8");
        }
        console.log(`[bundle-assets] Created ${outputBundleName}.min.js (${(fs.statSync(minFile).size / 1024).toFixed(1)} KB)`);
    } catch (err) {
        console.error(`[bundle-assets] Warning: Failed to minify ${outputBundleName}.js:`, err.message);
    }

    // Distribute
    destDirs.forEach((dir) => {
        if (fs.existsSync(dir)) {
            fs.writeFileSync(path.join(dir, `${outputBundleName}.js`), fs.readFileSync(bundleFile));
            if (fs.existsSync(minFile)) {
                fs.writeFileSync(path.join(dir, `${outputBundleName}.min.js`), fs.readFileSync(minFile));
            }
            console.log(`[bundle-assets] Copied JS bundles to ${dir}`);
        }
    });
}

function bundleCssFiles(srcDir, fileList, outputBundleName, destDirs) {
    console.log(`[bundle-assets] Bundling CSS (${srcDir}) -> ${outputBundleName}.css ...`);
    let bundleContent = licenseHeader + "\n";
    fileList.forEach((file) => {
        const filePath = path.join(srcDir, file);
        if (!fs.existsSync(filePath)) {
            console.error(`[bundle-assets] Error: File not found: ${filePath}`);
            process.exit(1);
        }
        let content = fs.readFileSync(filePath, "utf-8");
        content = content.replace(/^\/\*[\s\S]*?\*\/\r?\n*/, "").trim();
        bundleContent += content + "\n\n";
    });

    const bundleCss = bundleContent.trim() + "\n";
    const bundleFile = path.join(srcDir, `${outputBundleName}.css`);
    fs.writeFileSync(bundleFile, bundleCss, "utf-8");
    console.log(`[bundle-assets] Created ${outputBundleName}.css (${(fs.statSync(bundleFile).size / 1024).toFixed(1)} KB)`);

    // Minify with esbuild
    const minFile = path.join(srcDir, `${outputBundleName}.min.css`);
    try {
        console.log(`[bundle-assets] Minifying ${outputBundleName}.css with esbuild...`);
        execSync(`npx --yes esbuild "${bundleFile}" --minify --outfile="${minFile}"`, {
            stdio: "inherit"
        });
        const minContent = fs.readFileSync(minFile, "utf-8");
        if (!minContent.startsWith("/*")) {
            fs.writeFileSync(minFile, licenseHeader + minContent, "utf-8");
        }
        console.log(`[bundle-assets] Created ${outputBundleName}.min.css (${(fs.statSync(minFile).size / 1024).toFixed(1)} KB)`);
    } catch (err) {
        console.error(`[bundle-assets] Warning: Failed to minify ${outputBundleName}.css:`, err.message);
    }

    // Distribute
    destDirs.forEach((dir) => {
        if (fs.existsSync(dir)) {
            fs.writeFileSync(path.join(dir, `${outputBundleName}.css`), fs.readFileSync(bundleFile));
            if (fs.existsSync(minFile)) {
                fs.writeFileSync(path.join(dir, `${outputBundleName}.min.css`), fs.readFileSync(minFile));
            }
            console.log(`[bundle-assets] Copied CSS bundles to ${dir}`);
        }
    });
}

const targetVersion = process.argv[2] || null;

// 1. AppMon Assets (Auto-detect latest appmon@X.Y)
const appmonDir = getAssetDir("appmon", targetVersion);
console.log(`[bundle-assets] Target AppMon directory: ${path.relative(rootDir, appmonDir)}`);

const appmonJsSrc = path.join(appmonDir, "js");
const appmonCssSrc = path.join(appmonDir, "css");
const appmonJsFiles = [
    "base-client.js",
    "websocket-client.js",
    "polling-client.js",
    "traffic-painter.js",
    "dashboard-chart.js",
    "dashboard-viewer.js",
    "dashboard-builder.js"
];
const appmonCssFiles = [
    "appmon.css",
    "appmon-dark.css"
];
const appmonJsDestDirs = [
    appmonJsSrc,
    path.join(rootDir, "appmon-demo", "app", "webapps", "appmon", "assets", "js"),
    path.join(rootDir, "console-demo", "app", "webapps", "console", "assets", "appmon", "js")
];
const appmonCssDestDirs = [
    appmonCssSrc,
    path.join(rootDir, "appmon-demo", "app", "webapps", "appmon", "assets", "css"),
    path.join(rootDir, "console-demo", "app", "webapps", "console", "assets", "appmon", "css")
];

bundleJsFiles(appmonJsSrc, appmonJsFiles, "appmon-bundle", appmonJsDestDirs);
bundleCssFiles(appmonCssSrc, appmonCssFiles, "appmon-bundle", appmonCssDestDirs);

// 2. Console Assets (Auto-detect latest console@X.Y)
const consoleDir = getAssetDir("console", targetVersion);
console.log(`[bundle-assets] Target Console directory: ${path.relative(rootDir, consoleDir)}`);

const consoleJsSrc = path.join(consoleDir, "js");
const consoleCssSrc = path.join(consoleDir, "css");
const consoleJsFiles = [
    "apon-highlighter.js",
    "console-client.js"
];
const consoleCssFiles = [
    "console.css"
];
const consoleJsDestDirs = [
    consoleJsSrc,
    path.join(rootDir, "console-demo", "app", "webapps", "console", "assets", "js")
];
const consoleCssDestDirs = [
    consoleCssSrc,
    path.join(rootDir, "console-demo", "app", "webapps", "console", "assets", "css")
];

bundleJsFiles(consoleJsSrc, consoleJsFiles, "console-bundle", consoleJsDestDirs);
bundleCssFiles(consoleCssSrc, consoleCssFiles, "console-bundle", consoleCssDestDirs);

console.log("[bundle-assets] All assets bundling completed successfully.");
