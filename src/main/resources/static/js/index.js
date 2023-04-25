let websocket;
function onConnect(evt) {

    evt.preventDefault();

    if (websocket != null && websocket.readyState == WebSocket.CONNECTING) {
        alert("Already Connected");
        return;
    }

    document.getElementById("client_name").disabled = true;

    const name = document.getElementById("client_name").value;

    websocket = new WebSocket("ws://localhost:8080/ws/?name=" + name);
    websocket.onmessage = onMessage;
    websocket.onopen = onOpen;

    function onMessage(evt) {
        let msg = evt.data;
        // check list
        if (msg.indexOf("CLIENT_LIST") >= 0) {
            let list = msg.split("$");
            let ul = document.getElementById("client-list");
            ul.innerHTML = '';
            for (let i = 1; i < list.length; i++) {
                ul.innerHTML = ul.innerHTML + "<li>" + list[i] + "</li>";
            }
            return;
        }

        let ta = document.getElementsByTagName("textarea")[0];
        ta.readOnly = false;
        let delimiter = '';
        if (ta.innerHTML.trim().length != 0) {
            delimiter = '\n';
        }
        ta.innerHTML = ta.innerHTML + delimiter + msg;
        ta.readOnly = true;
        let scrollHeight = ta.scrollHeight;
        ta.scrollTop = scrollHeight;
    }


    //접속
    function onOpen(evt) {
        let str = name + ": connected";
        websocket.send(str);
    }

    return false;
}

//연결 안됐을 때
function onDisconnect() {
    if (websocket.readyState == WebSocket.CLOSED) {
        alert("Not Connected");
        return;
    }
    const name = document.getElementById("client_name").value;
    let str = name + ": disconnected";
    websocket.send(str);
    websocket.close();
    document.getElementById("client_name").disabled = false;
    document.getElementById("client-list").innerHTML = 'Disconnected';
}

window.onload = () => {
    document.getElementById("client_login_button").addEventListener("click", onConnect, false);
    document.getElementById("client_logout_button").addEventListener("click", onDisconnect, false);
}