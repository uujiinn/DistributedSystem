let websocket;
function onConnect(evt) {

    evt.preventDefault();

    if (websocket) {
        alert("Already Connected");
        return;
    }

    const name = document.getElementById("client_name").value;

    websocket = new WebSocket("ws://localhost:8080/ws/?name=" + name);
    // websocket.onmessage = onMessage;
    websocket.onopen = onOpen;
    websocket.onclose = onClose;

    function onMessage(evt) {
        let msg = evt.data;
        let ta = document.getElementsByTagName("textarea")[0];
        ta.readOnly = false;
        let delimiter='';
        if (ta.innerHTML.trim().length != 0) {
            delimiter = '\n';
        }
        ta.innerHTML = ta.innerHTML + delimiter + msg;
        ta.readOnly = true;
        let scrollHeight = ta.scrollHeight;
        ta.scrollTop = scrollHeight;
    }


    //채팅창에서 나갔을 때
    function onClose(evt) {
        let str = name + ": 님이 방을 나가셨습니다.";
        websocket.send(str);
    }

    //채팅창에 들어왔을 때
    function onOpen(evt) {
        let str = name + ": 님이 입장하셨습니다.";
        websocket.send(str);
    }

    return false;
}

window.onload = () => {
    document.getElementById("client_login_button").addEventListener("click", onConnect, false);
    document.getElementById("client_logout_button").addEventListener("click", () => {
        if (websocket == null) {
            alert("Not Connected");
            return;
        }
        const name = document.getElementById("client_name").value;
        let str = name + ": 님이 방을 나가셨습니다.";
        websocket.send(str);
        websocket.close();
        websocket = null;
    }, false);
}