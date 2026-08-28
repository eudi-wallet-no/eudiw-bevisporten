export function popover(message, event) {
    document.querySelector(".ds-popover")?.remove();

    const messageElement = document.createElement("div");

    messageElement.className = "ds-popover";
    messageElement.dataset.placement = "top";

    messageElement.textContent = message;
    messageElement.style.position = "fixed";
    messageElement.style.visibility = "hidden";

    document.body.appendChild(messageElement);

    const rect = messageElement.getBoundingClientRect();
    const padding = 10;
    const gap = 15;

    const left = Math.min(
        Math.max(event.clientX - rect.width / 2, padding),
        window.innerWidth - rect.width - padding
    );

    const top = Math.max(
        event.clientY - rect.height - gap,
        padding
    );

    messageElement.style.left = `${left}px`;
    messageElement.style.top = `${top}px`;
    messageElement.style.visibility = "visible";

    setTimeout(() => {
        messageElement.remove();
    }, 2000);
}

export function copyToClipboard(text, message, event) {
    navigator.clipboard.writeText(text).then(function() {
        popover(message, event);
    }, function(err) {
        console.error('Could not copy text: ', err);
    });
}