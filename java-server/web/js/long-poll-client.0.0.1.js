var console = {
    log: function (log) {
        $('#messageLog').prepend("<span class='messageLogRow'>[" + (new Date().toJSON().substr(0, 23)) + "] " + log + "</span>");
    }
};
var LongPollClient = {

        sessionId: "",
        username: "",
        lastConnectionTime: ((new Date()).getTime()),
        waitForLongPoll: 1000,
        maximumWaitMilliSeconds: 60000,
        shutdown: false,
        messageIdForAck: [],
        messageIdForGotId: [],

        connect: function () {
            LongPollClient.shutdown = false;
            var username = $('#messageUsername').val();
            var password = $('#messagePassword').val();
            var messageUrl = "http://" + LongPollClient.HOST + "/long-poll-server/Login?username=" + username + "&password=" + password + "&" + (Math.random());
            $.ajax({
                url: messageUrl,
                success: function (data, textStatus, jqXHR) {
                    LongPollClient.lastConnectionTime = ((new Date()).getTime());
                    console.log("login success, will do long poll, url: " + messageUrl + " data: " + JSON.stringify(data) + " textStatus: " + textStatus + " jqXHR: " + jqXHR);
                    if (data["loginStatus"] === "success") {
                        LongPollClient.sessionId = data["sessionId"];
                        LongPollClient.username = username;
                        LongPollClient.doLongPoll();
                    } else {
                        alert("wrong username / password");
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.log("login failed error: jqXHR: " + jqXHR + ", textStatus: " + textStatus + ", errorThrown:" + errorThrown);
                },
                timeout: LongPollClient.maximumWaitMilliSeconds,
                dataType: "json"
            });
        },

        addLine: function (isMineOrFromFriend, message) {

            if (isMineOrFromFriend === true) {
                $("#LongPollContainer").append("<div class=\"messageMyRow\"><span class=\"messageDate\">" + "(" + message["date"] + ")" + "</span><span id=\"" + message["messageId"] + "\" class=\"messageStatusSending\"></span><span class=\"messageGreen messageRoundedCorners\">" + message["content"] + "</span></div>");
            } else {
                // status of the message is "0"
                // this is a normal message someone sending to us, show it on the screen
                if (message["status"] === "0") {
                    if (message["from"] === $.trim($('#messageToUser').val())) {
                        $("#LongPollContainer").append("<div class=\"messageFriendRow\"><span id=\"" + message["messageId"] + "\" class=\"\"></span><span class=\"messageBlue messageRoundedCorners\">" + Base64.decode(message["content"]) + "</span><span class=\"messageDate\">" + "(" + message["date"] + ")" + "</span></div>");
                    } else {
                        // TODO: store this message at the client side Storage and show notification about this message
                        console.log("message from someone else, show notification and store it, message: " + JSON.stringify(message));
                    }
                }
                if (message["status"] === "1") {
                    if (message["from"] === $.trim($('#messageToUser').val())) {
                        if ($("#" + message["messageId"]).attr('id') !== undefined) {
                            console.log("row found with id: " + message["messageId"]);
                        } else {
                            console.log("row not found, will add row");
                            $("#LongPollContainer").append("<div class=\"messageFriendRow\"><span id=\"" + message["messageId"] + "\" class=\"\"></span><span class=\"messageBlue messageRoundedCorners\">" + Base64.decode(message["content"]) + "</span><span class=\"messageDate\">" + "(" + message["date"] + ")" + "</span></div>");
                        }
                    } else {
                        // TODO: store this message at the client side Storage and show notification about this message
                        console.log("message from someone else, show notification and store it, message: " + JSON.stringify(message));
                    }
                }
                // "0" and "1" send an acknowledge to this message
                if (message["status"] === "0" || message["status"] === "1") {
                    console.log("send response message to server indicating that this message is received");
                    LongPollClient.messageIdForAck.push(message["messageId"]);
                    LongPollClient.sendAcknowledgementMessages();
                }
                // "2" we have send this message to someone, and that person read the message and send an acknowledgement
                // change the icon to double tick
                if (message["status"] === "3") {
                    $("#" + message["messageId"]).attr('class', 'messageStatusReceived');
                    // TODO: update this message at the client side Storage
                    console.log("the friend read the message, this is the acknowledgement, message: " + JSON.stringify(message));
                    LongPollClient.messageIdForGotId.push(message["messageId"]);
                    LongPollClient.sendAcknowledgementMessages();
                }

            }
        },


        sendAcknowledgementMessages: function () {

            // first send the status 0 and 1 messages
            while (LongPollClient.messageIdForAck.length > 0) {
                var messageId = LongPollClient.messageIdForAck.pop();
                LongPollClient.lastConnectionTime = ((new Date()).getTime());
                console.log("will send an acknowledgement message for this message id: " + messageId + " array size: " + LongPollClient.messageIdForAck.length);
                var messageUrl = "http://" + LongPollClient.HOST + "/long-poll-server/AckMessage?sessionId=" + LongPollClient.sessionId + "&messageId=" + messageId + "&" + (Math.random());
                $.ajax({
                    url: messageUrl,
                    success: function (data, textStatus, jqXHR) {
                        console.log("successfully sent the acknowledgement message for this message id: " + messageId + " removing it from array size: " + LongPollClient.messageIdForAck.length);
                        LongPollClient.lastConnectionTime = ((new Date()).getTime());
                        if (data["status"] === "updated") {
                            console.log("status updated of the message with messageId: " + data["messageId"]);
                            // TODO: update this message's status at the client side Storage
                        }
                        console.log("acknowledge success, message send: " + messageUrl + " data: " + data + " textStatus: " + textStatus + " jqXHR: " + jqXHR);
                    },
                    error: function (jqXHR, textStatus, errorThrown) {
                        LongPollClient.messageIdForAck.push(messageId);
                        console.log("acknowledge error, push messageId to array: " + messageId + " array size:" + LongPollClient.messageIdForAck.length + " message error: jqXHR: " + jqXHR + ", textStatus: " + textStatus + ", errorThrown:" + errorThrown);
                    },
                    timeout: LongPollClient.maximumWaitMilliSeconds,
                    dataType: "json"
                });
            }

            // and re-send the got it message
            while (LongPollClient.messageIdForGotId.length > 0) {
                var messageId = LongPollClient.messageIdForGotId.pop();
                LongPollClient.lastConnectionTime = ((new Date()).getTime());
                console.log("will send an got it message for this message id: " + messageId + " array size: " + LongPollClient.messageIdForGotId.length);
                var messageUrl = "http://" + LongPollClient.HOST + "/long-poll-server/ReceivedAckMessage?sessionId=" + LongPollClient.sessionId + "&messageId=" + messageId + "&" + (Math.random());
                $.ajax({
                    url: messageUrl,
                    success: function (data, textStatus, jqXHR) {
                        console.log("successfully sent the got it message for this message id: " + messageId + " removing it from array size: " + LongPollClient.messageIdForGotId.length);
                        LongPollClient.lastConnectionTime = ((new Date()).getTime());
                        if (data["status"] === "acknowledged") {
                            console.log("status acknowledged of the message with messageId: " + data["messageId"]);
                            // TODO: update this message's status at the client side Storage
                        }
                        console.log("got it success, message send: " + messageUrl + " data: " + data + " textStatus: " + textStatus + " jqXHR: " + jqXHR);
                    },
                    error: function (jqXHR, textStatus, errorThrown) {
                        LongPollClient.messageIdForGotId.push(messageId);
                        console.log("got it error, pushed messageId to array: " + messageId + " array size:" + LongPollClient.messageIdForGotId.length + " message error: jqXHR: " + jqXHR + ", textStatus: " + textStatus + ", errorThrown:" + errorThrown);
                    },
                    timeout: LongPollClient.maximumWaitMilliSeconds,
                    async: true,
                    dataType: "json"
                });
            }

        },

        doLongPoll: function () {
            var doLongPollFunction = function () {
                if (LongPollClient.shutdown === true) {
                    console.log("someone else logged in, connection closed");
                    alert("someone else logged in, connection closed");
                    $("#messageConnect").attr("disabled", false);
                    $("#LongPollSend").find("input").attr("disabled", true);
                } else {
                    // wait and retry
                    console.log("will wait and retry long poll");
                    setTimeout('LongPollClient.doLongPoll()', LongPollClient.waitForLongPoll);
                }
            };
            $.ajax({
                    url: "http://" + LongPollClient.HOST + "/long-poll-server/GetMessage?sessionId=" + LongPollClient.sessionId + "&" + (Math.random()),
                    success: function (data, textStatus, jqXHR) {
                        LongPollClient.lastConnectionTime = ((new Date()).getTime());
                        if (data["shutdown"] === "true") {
                            LongPollClient.shutdown = true;
                            console.log("another one logged in, shutdown");

                        } else {
                            var messagesArray = data;
                            if (messagesArray !== null) {
                                if (messagesArray.length > 0) {
                                    for (var i = 0; i < messagesArray.length; i++) {
                                        var message = messagesArray[i];
                                        LongPollClient.addLine(false, message);
                                        $('#LongPollContainer').scrollTop($('#LongPollContainer')[0].scrollHeight);
                                    }
                                } else {
                                    console.log("success but there are no messages: jqXHR: " + jqXHR + ", textStatus: " + textStatus + ", data:" + data + " JSON: " + JSON.stringify(data));
                                }
                            } else {
                                console.log("response from server is NULL")
                            }
                        }

                        // do long poll
                        doLongPollFunction();
                    },
                    error: function (jqXHR, textStatus, errorThrown) {
                        console.log("long poll error: jqXHR: " + JSON.stringify(jqXHR) + " responseText: " + jqXHR["responseText"] + ", textStatus: " + textStatus + ", errorThrown:" + errorThrown);
                        var responseText = jQuery.parseJSON(jqXHR["responseText"]);
                        if (responseText !== null && responseText["shutdown"] === "true") {
                            LongPollClient.shutdown = true;
                            console.log("another one logged in, shutdown");
                        } else {
                            console.log("error occurred, but will wait and retry long poll");
                        }

                        // do long poll
                        doLongPollFunction();
                    },
                    timeout: LongPollClient.maximumWaitMilliSeconds,
                    dataType: "json"
                }
            )
            ;
        },

        sendMessage: function (sendMessageContent) {
            var messageContent = $.trim($('#messageText').val());
            if (sendMessageContent !== null && sendMessageContent !== undefined) {
                messageContent = sendMessageContent;
            }
            var toUser = $('#messageToUser').val();
            var now = new Date();
            var messageDate =
                (now.getHours() < 10 ? "0" + now.getHours() : now.getHours()) + ':' +
                    (now.getMinutes() < 10 ? "0" + now.getMinutes() : now.getMinutes()) + " " +
                    (now.getDay() < 10 ? "0" + now.getDay() : now.getDay()) + "/" +
                    ((now.getMonth() + 1) < 10 ? "0" + (now.getMonth() + 1) : (now.getMonth() + 1)) + "/" +
                    now.getFullYear();
            var messageId = LongPollClient.username + "-" + (now.getTime());
            if (messageContent != "") {
                LongPollClient.addLine(true, {"content": messageContent, "messageId": messageId, "date": messageDate, "from": LongPollClient.sessionId});
                $('#LongPollContainer').scrollTop($('#LongPollContainer')[0].scrollHeight);
                messageContent = Base64.encode(messageContent);
                var messageUrl = "http://" + LongPollClient.HOST + "/long-poll-server/SendMessage?sessionId=" + LongPollClient.sessionId + "&to=" + toUser + "&content=" + messageContent + "&messageId=" + messageId + "&" + (Math.random());
                $.ajax({
                    url: messageUrl,
                    success: function (data, textStatus, jqXHR) {
                        LongPollClient.lastConnectionTime = ((new Date()).getTime());
                        if (data["status"] !== undefined && data["status"] === "stored") {
                            console.log("message send: " + messageUrl + " data: " + data + " textStatus: " + textStatus + " jqXHR: " + jqXHR);
                            $("#" + messageId).attr('class', 'messageStatusSent');
                        } else {
                            console.log("message send failed at the server side, will retry messageUrl: " + messageUrl + " data: " + data + " textStatus: " + textStatus + " jqXHR: " + jqXHR);
                            LongPollClient.sendMessage(messageContent);
                        }
                    },
                    error: function (jqXHR, textStatus, errorThrown) {
                        console.log("send message error: jqXHR: " + jqXHR + ", textStatus: " + textStatus + ", errorThrown:" + errorThrown);
                    },
                    timeout: LongPollClient.maximumWaitMilliSeconds,
                    dataType: "json"
                });
            }
            $('#messageText').val("");
        }

    }
    ;


$(document).ready(function () {

    LongPollClient.HOST = window.location.host;
    LongPollClient.maximumWaitMilliSeconds = 60 * 1000;
    LongPollClient.waitForLongPoll = 1000;

    $("#LongPollSend").find("input").attr("disabled", true);

    $('#messageText').keypress(function (event) {
        if (event.which == 13) {
            LongPollClient.sendMessage();
        }
    });

    $('#messageButton').bind('click', function () {
        LongPollClient.sendMessage();
    });

    $('#messageConnect').bind('click', function () {
        $("#messageConnect").attr("disabled", true);
        LongPollClient.connect();
        $("#LongPollSend").find("input").attr("disabled", false);
        $(function () {
            $("#messageText").focus();
        });

    });

});
