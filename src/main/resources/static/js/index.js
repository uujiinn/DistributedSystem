let websocket;

function onConnect(evt) {

    evt.preventDefault();

    if (websocket != null && websocket.readyState == WebSocket.CONNECTING) {
        alert("Already Connected");
        return;
    }

    document.getElementById("client_login_button").disabled = true;
    document.getElementById("client_name").disabled = true;
    document.getElementById("file-form-wrapper").style.display = "block";
    document.getElementById("file-list").style.display = "block";

    const name = document.getElementById("client_name").value;

    websocket = new WebSocket("ws://localhost:8080/ws/?name=" + name);
    websocket.onmessage = onMessage;
    websocket.onopen = onOpen;

    document.getElementById("client_name_input").value = name;

    function onMessage(evt) {
        let msg = evt.data;

        if (msg.indexOf("You received") >= 0) {
            websocket.send("REQ_EMIT");
        }
        // check client list
        if (msg.indexOf("CLIENT_LIST") >= 0) {
            let list = msg.split("$");
            let ul = document.getElementById("client-list");
            ul.innerHTML = '';

            const name = document.getElementById("client_name").value;
            let selectClient = document.getElementById("selected_client");
            selectClient.innerHTML = '<option value="">Select a client</option>';

            for (let i = 1; i < list.length; i++) {
                ul.innerHTML = ul.innerHTML + "<li>" + list[i] + "</li>";

                if (list[i] != name && list[i].length != 0) {
                    selectClient.innerHTML += '<option value="' + list[i] + '">' + list[i] + '</option>';
                }
            }
            return;
        }

        // check file list - client
        if (msg.indexOf("CLIENT_FILE_LIST") >= 0) {
            let list = msg.split("$");
            let ul = document.getElementById("file-list");
            ul.innerHTML = '';

            let selectFile = document.getElementById("selected_file");
            selectFile.innerHTML = '<option value="">Select a file</option>';

            let owner = "[CLIENT]";
            for (let i = 1; i < list.length && list[i].length != 0; i++) {
                if (list[i] == "SERVER_FILE_LIST") {
                    owner = "[SERVER]";
                    continue;
                }
                ul.innerHTML = ul.innerHTML +
                    "<div id='file_item'>" +
                    "<li>" + owner + list[i] +
                    "</li>" +
                    "<button type='button' onclick='onDelete(event)' data-filename='" +
                    owner + list[i] +
                    "'" +
                    ">" +
                    "Delete</button>" +
                    "</div>";
                if (owner == "[CLIENT]")
                    selectFile.innerHTML += '<option value="' + list[i] + '">'  + list[i] + '</option>';

            }

            return;
        }

        //check file list - server
        if (msg.indexOf("SERVER_FILE_LIST") >= 0) {
            let list = msg.split("$");
            let ul = document.getElementById("file-list");
            // ul.innerHTML = '';

            for (let i = 1; i < list.length && list[i].length != 0; i++) {
                ul.innerHTML = ul.innerHTML +
                    "<div id='file_item'>" +
                    "<li>" + "[SERVER]" + list[i] +
                    "</li>" +
                    "<button type='button' onclick='onDelete(event)' data-filename='" +
                    list[i] +
                    "'" +
                    ">" +
                    "Delete</button>" +
                    "</div>";
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

function onDelete(evt) {

    console.log(evt);
    const name = document.getElementById("client_name").value;
    let filename = evt.target.dataset.filename;
    console.log(name + "," + filename);
    filename = filename.replace("[", "").replace("]", "");
    let url = '/deleteFile?host=' + name + '&filename=' + filename;
    console.log(url);
    fetch(url, {
        method: 'GET',
    }).then(response => {
        // 서버 응답 처리
        console.log('파일 업로드 성공');
    }).catch(error => {
        console.error('파일 업로드 실패', error);
    });
}

function onSync(evt){
    let msg = evt.data;
    const name = document.getElementById("client_name").value;
    websocket.send("SYNC:" + name);
}

function onShare(evt) {
    const sender = document.getElementById("client_name").value;
    const selectedFile = document.getElementById("selected_file").value;
    const selectedClient = document.getElementById("selected_client").value;

    if (!selectedFile || !selectedClient) {
        alert("Please select a fil./e and a client");
        return;
    }

    const message = `SHARE_FILE:${sender}:${selectedFile}:${selectedClient}`;
    websocket.send(message);
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
    document.getElementById("share_button").addEventListener("click", onShare, false);
    document.getElementById("sync_button").addEventListener("click", onSync, false);

    const form = document.getElementById('file-form-wrapper');
    const fileInput = document.getElementById('input_file');
    const clientNameInput = document.getElementById('client_name_input');
    form.addEventListener("submit", (evt) => {
        evt.preventDefault(); // 기본 이벤트 방지

        const formData = new FormData(); // FormData 객체 생성
        formData.append('input_file', fileInput.files[0]); // 파일 추가
        formData.append('client_name', clientNameInput.value); // 숨은 입력 필드 추가

        fetch('/file', {
            method: 'POST',
            body: formData // FormData 객체를 전송 데이터로 사용
        })
            .then(response => {
                // 서버 응답 처리
                console.log('파일 업로드 성공');
            })
            .catch(error => {
                console.error('파일 업로드 실패', error);
            });
    });
}