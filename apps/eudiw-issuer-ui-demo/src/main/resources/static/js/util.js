export function popover(message, event) {
    const messageElement = document.createElement("div");
    messageElement.style.position = "fixed";

    messageElement.className = "ds-popover"
    message.data.placement = "top";

    messageElement.textContent = message;
    messageElement.style.visibility = "hidden";

    document.body.appendChild(messageElement);

    const rect = messageElement.getBoundingClientRect();

    messageElement.style.top = `${event.clientY - rect.height - 15}px`;
    messageElement.style.left = `${event.clientX - (rect.width / 2)}px`;

    messageElement.style.visibility = "visible";

    setTimeout(() => {
        messageElement.remove();
    }, 20000);
}

export function copyToClipboard(text, message, event) {
    navigator.clipboard.writeText(text).then(function() {
        popover(message, event);
    }, function(err) {
        console.error('Could not copy text: ', err);
    });
}