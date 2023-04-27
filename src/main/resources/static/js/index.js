let websocket;
function onConnect(evt) {

    evt.preventDefault();

    if (websocket != null && websocket.readyState == WebSocket.CONNECTING) {
        alert("Already Connected");
        return;
    }

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

        // check client list
        if (msg.indexOf("CLIENT_LIST") >= 0) {
            let list = msg.split("$");
            let ul = document.getElementById("client-list");
            ul.innerHTML = '';
            for (let i = 1; i < list.length; i++) {
                ul.innerHTML = ul.innerHTML + "<li>" + list[i] + "</li>";
            }
            return;
        }

        // check file list
        if (msg.indexOf("FILE_LIST") >= 0) {
            let list = msg.split("$");
            let ul = document.getElementById("file-list");
            ul.innerHTML = '';
            for (let i = 1; i < list.length && list[i].length != 0; i++) {
                ul.innerHTML = ul.innerHTML +
                    "<div id='file_item'>" +
                    "<li>" + list[i] +
                    "</li>" +
                    "<button type='button' onclick='onDelete(event)' data-filename='" +
                    list[i] +
                    "'" +
                    "데이터 삭제"+
                    "</button>"+
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