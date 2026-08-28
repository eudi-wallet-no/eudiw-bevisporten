export function displayMessage(message, event) {
    const messageElement = document.createElement("div");
    messageElement.className = "mini_message"
    messageElement.textContent = message;


    messageElement.style.top = `${event.clientY + 10}px`;
    messageElement.style.left = `${event.clientX + 10}px`;

    document.body.appendChild(messageElement);

    setTimeout(() => {
        messageElement.remove();
    }, 2000);
}

export function copyToClipboard(text, message, event) {
    navigator.clipboard.writeText(text).then(function() {
        displayMessage(message, event);
    }, function(err) {
        console.error('Could not copy text: ', err);
    });
}