var username;

window.onload = function () {

    function clearMessages() {
        const chat_messages = document.getElementById("chat-messages");
        chat_messages.innerHTML = "";
    }

    function addMessages(messages) {
        const chat_messages = document.getElementById("chat-messages");
        for (let message of messages) {
            let fromUser = message.fromUser;
            let msg = message.message;
            let toUser = message.toUser;
            let html;
            if (fromUser) {
                if (toUser)
                    html = '<div>' + '@' + fromUser + '>@' + toUser + ': ' + msg + '</div>';
                else
                    html = '<div>' + '@' + fromUser + ': ' + msg + '</div>'
            } else
                html = '<div>' + msg + '</div>'
            chat_messages.insertAdjacentHTML("beforeend", html);
        }
        // chat_messages.scrollTop = chat_messages.scrollHeight;
    }

    var socket = new WebSocket("ws://localhost:8888/websocket");

    // socket.onopen = function () {
    //     socket.send("open");
    // }

    /*
    join response:
    {
        "username": "Ivan",
        "status": 0,
        "messages": [
            {
                "fromUser": "Alice",
                "message": "Hello, Bob",
                "toUser: "Bob"
            },
            {
                "fromUser": "Bob",
                "message": "Hello everyone"
                "toUser": null
            }
        ]
    }

    message:
    {
        "fromUser": "Alice",
        "message": "Hello, Bob",
        "toUser: "Bob"
    }

    error:
    {
        "code": 1,
        "message": "Нет пользователя с именем Bob"
    }
     */
    socket.onmessage = function (event) {
        let data = JSON.parse(event.data);
        if ("username" in data) { // join response
            if (data.status === 0) {
                username = data.username;
                clearMessages();
                addMessages(data.messages);
            } else
                alert("status: " + data.status);

        } else if ("code" in data) { // error
            alert(data.message);

        } else { // message
            addMessages([data]);
        }
    }

    socket.onclose = function (event) {
        alert("close");
    }

    // socket.onerror = function (error) {
    // }

    const joinForm = document.getElementById("chat-join-form");
    joinForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const input = joinForm.getElementsByTagName("input")[0];
        if (input.value) {
            let joinRequest = {
                username: input.value
            };
            socket.send(JSON.stringify(joinRequest));

            input.value = "";
        }
    });


    const sendForm = document.getElementById("chat-send-form");
    sendForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const input = sendForm.getElementsByTagName("input")[0];
        if (input.value) {
            if (username) {
                let msg = input.value;
                let toUser = null;
                if (msg.startsWith("@")) {
                    let sp = msg.indexOf(' ');
                    toUser = msg.substring(1, sp);
                    msg = msg.substring(sp + 1);
                }
                let message = {
                    fromUser: username,
                    message: msg,
                    toUser: toUser
                }
                socket.send(JSON.stringify(message));

                input.value = "";
            } else
                alert("Вы не присоединилсь");
        }

    });
}