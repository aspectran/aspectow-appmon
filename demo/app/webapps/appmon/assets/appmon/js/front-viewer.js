function FrontViewer() {
    let $displays = {};
    let $consoles = {};
    let $indicators = {};
    let visible = false;
    let prevPosition = 0;
    let currentActivities = [];

    this.putDisplay = function (instance, label, $display) {
        $displays[instance + ":event:" + label] = $display;
    };

    this.putConsole = function (instance, label, $console) {
        $consoles[instance + ":log:" + label] = $console;
    };

    this.putIndicator = function (instance, type, label, $indicator) {
        $indicators[instance + ":" + type + ":" + label] = $indicator;
    };

    const getDisplay = function (name) {
        return ($displays && name ? $displays[name] : null);
    };

    const getConsole = function (name) {
        return ($consoles && name ? $consoles[name] : null);
    };

    this.refreshConsole = function ($console) {
        if ($console) {
            scrollToBottom($console);
        } else {
            for (let key in $consoles) {
                if (!$consoles[key].data("pause")) {
                    scrollToBottom($consoles[key]);
                }
            }
        }
    };

    this.clearConsole = function ($console) {
        if ($console) {
            $console.empty();
        }
    };

    const scrollToBottom = function ($console) {
        if ($console && $console.data("tailing")) {
            let timer = $console.data("timer");
            if (timer) {
                clearTimeout(timer);
            }
            timer = setTimeout(function () {
                $console.scrollTop($console.prop("scrollHeight"));
                if ($console.find("p").length > 11000) {
                    $console.find("p:gt(10000)").remove();
                }
            }, 300);
            $console.data("timer", timer);
        }
    };

    this.setVisible = function (flag) {
        visible = !!flag;
        if (!visible) {
            for (let key in $displays) {
                if ($displays[key].hasClass("track-box")) {
                    $displays[key].find(".bullet").remove();
                }
            }
        }
    };

    this.printMessage = function (msg, name) {
        if (name) {
            let $console = getConsole(name);
            $("<p/>").addClass("event ellipses").html(msg).appendTo($console);
            scrollToBottom($console);
        } else {
            for (let key in $consoles) {
                this.printMessage(msg, key);
            }
        }
    };

    this.printErrorMessage = function (msg, name) {
        if (name || !Object.keys($consoles).length) {
            let $console = getConsole(name);
            $("<p/>").addClass("event error").html(msg).appendTo($console);
            scrollToBottom($console);
        } else {
            for (let key in $consoles) {
                this.printErrorMessage(msg, key);
            }
        }
    };

    this.processMessage = function (msg) {
        let idx1 = msg.indexOf(":");
        let idx2 = msg.indexOf(":", idx1 + 1);
        let idx3 = msg.indexOf(":", idx2 + 1);
        let instance = msg.substring(0, idx1);
        let type = msg.substring(idx1 + 1, idx2);
        let label = msg.substring(idx2 + 1, idx3);
        let name = msg.substring(0, idx3);
        let text = msg.substring(idx3 + 1);
        switch (type) {
            case "event":
                if (text.length) {
                    indicate(instance, type, label);
                    processEventData(label, name, JSON.parse(text));
                }
                break;
            case "log":
                indicate(instance, type, label);
                printLogMessage(name, text);
                break;
        }
    };

    const printLogMessage = function (name, text) {
        let $console = getConsole(name);
        if (!$console.data("pause")) {
            $("<p/>").text(text).appendTo($console);
            scrollToBottom($console);
        }
    };

    const processEventData = function (label, name, data) {
        switch (label) {
            case "activity":
                if (data.activities) {
                    printActivities(name, data.activities);
                }
                if (visible) {
                    let $track = getDisplay(name);
                    if ($track) {
                        let varName = name.replace(':', '_');
                        if (!currentActivities[varName]) {
                            currentActivities[varName] = 0;
                            printCurrentActivities(name, 0);
                        }
                        launchBullet($track, data, function () {
                            currentActivities[varName]++;
                            printCurrentActivities(name, currentActivities[varName]);
                        }, function () {
                            if (currentActivities[varName] > 0) {
                                currentActivities[varName]--;
                            }
                            printCurrentActivities(name, currentActivities[varName]);
                        });
                    }
                } else {
                    printCurrentActivities(name, 0);
                }
                break;
            case "session":
                printSessionEventData(name, data);
                break;
        }
    }

    const launchBullet = function ($track, data, onLeaving, onArriving) {
        if (data.elapsedTime) {
            if (onLeaving) {
                onLeaving();
            }
            let position = generateRandom(3, 103);
            if (prevPosition) {
                if (Math.abs(position - prevPosition) <= 20) {
                    position = generateRandom(3, 103);
                    if (Math.abs(position - prevPosition) <= 20) {
                        position = generateRandom(3, 103);
                    }
                }
            }
            prevPosition = position;
            let $bullet = $("<div class='bullet'/>")
                .attr("sessionId", data.sessionId)
                .css("top", position + "px")
                .appendTo($track).show();
            setTimeout(function () {
                $bullet.addClass("arrive");
                setTimeout(function () {
                    $bullet.fadeOut(1000);
                    setTimeout(function () {
                        $bullet.remove();
                        if (onArriving) {
                            onArriving();
                        }
                    }, 500);
                }, data.elapsedTime + 350);
            }, 900);
        }
    };

    const generateRandom = function (min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    };

    const indicate = function (instance, type, label) {
        let $indicator1 = $indicators["endpoint:event:"];
        blink($indicator1);
        if (visible) {
            if (type === "log") {
                let $indicator3 = $indicators[instance + ":log:" + label];
                blink($indicator3);
            } else {
                let $indicator2 = $indicators["instance:event:" + instance];
                blink($indicator2);
            }
        }
    };

    const blink = function ($indicator) {
        if ($indicator && !$indicator.hasClass("on")) {
            $indicator.addClass("blink on");
            setTimeout(function () {
                $indicator.removeClass("blink on");
            }, 500);
        }
    }

    const printActivities = function (name, data) {
        let $activities = $indicators[name];
        if ($activities) {
            $activities.find(".total").text(data.total);
        }
    }

    const printCurrentActivities = function (name, current) {
        let $activities = $indicators[name];
        if ($activities) {
            $activities.find(".current").text(current);
        }
    }

    const printSessionEventData = function (name, data) {
        let $display = getDisplay(name);
        if ($display) {
            $display.find(".numberOfCreated").text(data.numberOfCreated);
            $display.find(".numberOfExpired").text(data.numberOfExpired);
            $display.find(".numberOfActives").text(data.numberOfActives);
            $display.find(".highestNumberOfActives").text(data.highestNumberOfActives);
            $display.find(".numberOfUnmanaged").text(data.numberOfUnmanaged);
            $display.find(".numberOfRejected").text(data.numberOfRejected);
            $display.find(".elapsed").text(data.elapsedTime);
            let $sessions = $display.find("ul.sessions");
            if (data.createdSessions) {
                data.createdSessions.forEach(function (session) {
                    addSessionItem($sessions, session);
                });
            }
            if (data.destroyedSessions) {
                data.destroyedSessions.forEach(function (sessionId) {
                    $sessions.find("li[data-sid='" + sessionId + "']").remove();
                });
            }
            if (data.evictedSessions) {
                data.evictedSessions.forEach(function (sessionId) {
                    let $item = $sessions.find("li[data-sid='" + sessionId + "']").addClass("inactive");
                    setTimeout(function () {
                        $item.addClass("hidden");
                    }, 30000);
                });
            }
            if (data.residedSessions) {
                data.residedSessions.forEach(function (session) {
                    addSessionItem($sessions, session);
                });
            }
        }
    };

    const addSessionItem = function ($sessions, session) {
        $sessions.find("li[data-sid='" + session.sessionId + "']").remove();
        let $indicator = $("<div/>").addClass("indicator");
        if (!session.username) {
            $indicator.addClass("logged-out")
        }
        let $item = $("<li/>").attr("data-sid", session.sessionId).append($indicator).appendTo($sessions);
        if (session.countryCode) {
            $("<img class='flag' alt=''/>")
                .attr("src", "https://aspectran.com/assets/countries/flags/" + session.countryCode.toLowerCase() + ".png")
                .attr("alt", session.countryCode)
                .attr("title", countries[session.countryCode].name + " / " + session.ipAddress)
                .appendTo($item);
        }
        let str = "Session <strong>" + session.sessionId + "</strong> created at <strong>" + session.createAt + "</strong>";
        if (session.username) {
            str = "(<strong>" + session.username + "</strong>) " + str;
        }
        $("<span/>").html(str).appendTo($item);
    };
}
