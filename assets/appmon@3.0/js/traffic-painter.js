/*
 * Aspectow AppMon 3.0
 * Last modified: 2026-02-15
 */

/**
 * Advanced Canvas-based particle engine for AppMon traffic visualization.
 * Handles tab visibility to prevent "bullet bursts" when returning to the tab.
 */
class TrafficPainter {
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
        this.bullets = [];
        this.animationId = null;
        this.isRunning = false;
        this.finishLineOffset = 180; // track-stack width

        this.resize();
        this.resizeObserver = new ResizeObserver(() => this.resize());
        this.resizeObserver.observe(this.canvas.parentElement);
    }

    resize() {
        const rect = this.canvas.parentElement.getBoundingClientRect();
        this.canvas.width = rect.width;
        this.canvas.height = rect.height;
    }

    /**
     * Adds a new bullet to the painter.
     * @param {Object} data - Bullet data (error, elapsedTime, activityCount).
     * @param {Function} onArriving - Callback when bullet reaches the finish line.
     */
    addBullet(data, onArriving) {
        const elapsedTime = data.elapsedTime || 0;
        const activityCount = data.activityCount || 0;
        const hasError = !!(data.error);
        const timeIntensity = Math.min(elapsedTime / 5000, 1);
        const targetMax = 1000;
        const activityIntensity = activityCount > 0 
            ? Math.min(Math.log10(activityCount + 1) / Math.log10(targetMax + 1), 1)
            : 0;
        
        const size = 3.0 + (timeIntensity * 4) + (activityIntensity * 4);
        const baseSpeed = (this.canvas.width - this.finishLineOffset) / (900 / 16.6);
        const speed = baseSpeed * (1 - (timeIntensity * 0.6));

        const bullet = {
            x: -(Math.random() * 150),
            y: Math.random() * (this.canvas.height - 20) + 10,
            speed: speed,
            size: size,
            timeIntensity: timeIntensity,
            activityIntensity: activityIntensity,
            color: hasError ? '#ff0000' : (timeIntensity > 0.5 ? '#f1c40f' : '#11d539'),
            elapsedTime: Math.max(elapsedTime, 500),
            arrived: false,
            arrivedTime: 0,
            impactPulse: 0,
            alpha: 1.0,
            onArriving: onArriving
        };
        this.bullets.push(bullet);

        if (!this.isRunning) {
            this.start();
        }
    }

    start() {
        this.isRunning = true;
        this.lastTime = performance.now();
        const loop = (currentTime) => {
            if (document.hidden) {
                this.isRunning = false;
                return;
            }
            
            const deltaTime = Math.min((currentTime - this.lastTime) / 16.66, 3.0);
            this.lastTime = currentTime;

            this.update(deltaTime);
            this.draw();

            if (this.bullets.length > 0) {
                this.animationId = requestAnimationFrame(loop);
            } else {
                this.isRunning = false;
                this.clear();
            }
        };
        this.animationId = requestAnimationFrame(loop);
    }

    update(deltaTime) {
        const finishLine = this.canvas.width - this.finishLineOffset;
        const now = Date.now();

        for (let i = this.bullets.length - 1; i >= 0; i--) {
            const b = this.bullets[i];

            if (!b.arrived) {
                b.x += b.speed * deltaTime;
                if (b.x >= finishLine) {
                    b.x = finishLine;
                    b.arrived = true;
                    b.arrivedTime = now;
                    b.impactPulse = 1.0;
                }
            } else {
                if (b.impactPulse > 0) {
                    b.impactPulse -= 0.05 * deltaTime;
                }

                const stayElapsed = now - b.arrivedTime;
                if (stayElapsed > b.elapsedTime + 200) {
                    b.alpha -= 0.04 * deltaTime;
                    if (b.alpha <= 0) {
                        if (b.onArriving) b.onArriving();
                        this.bullets.splice(i, 1);
                    }
                }
            }
        }
    }

    draw() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        for (let i = 0; i < this.bullets.length; i++) {
            const b = this.bullets[i];
            this.ctx.globalAlpha = b.alpha;
            
            const drawSize = b.arrived ? b.size * (1 + b.impactPulse * 0.3) : b.size;

            // 1. Aura (Glow) - high performance alternative to shadowBlur
            this.ctx.fillStyle = b.color;
            this.ctx.beginPath();
            let auraSize = drawSize * (1.2 + b.timeIntensity + (b.arrived ? b.impactPulse : 0));
            this.ctx.globalAlpha = b.alpha * 0.3;
            this.ctx.arc(b.x, b.y, auraSize, 0, Math.PI * 2);
            this.ctx.fill();

            // 2. Main Bullet Body
            this.ctx.globalAlpha = b.alpha;
            this.ctx.beginPath();
            this.ctx.arc(b.x, b.y, drawSize, 0, Math.PI * 2);
            this.ctx.fill();

            // 3. Activity Glow (Hot Core) - high performance alternative to radial gradient
            if (b.activityIntensity > 0.3) {
                const coreSize = drawSize * (0.3 + b.activityIntensity * 0.4);
                this.ctx.fillStyle = '#fff';
                this.ctx.beginPath();
                this.ctx.arc(b.x, b.y, coreSize, 0, Math.PI * 2);
                this.ctx.fill();
            }
        }
        this.ctx.globalAlpha = 1.0;
    }

    clear() {
        this.bullets = [];
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }

    destroy() {
        if (this.animationId) cancelAnimationFrame(this.animationId);
        this.resizeObserver.disconnect();
    }
}
