function addClaim() {
    const container = document.getElementById("claims");
    const index = container.children.length;

    const template = document.getElementById("claim-template").innerHTML;
    const html = template.replaceAll("INDEX", `${index}`);
    container.insertAdjacentHTML("beforeend", html);
}

function removeClaim(button) {
    button.parentElement.remove();
    reindexClaims();
}

function reindexClaims() {
    const claims = document.querySelectorAll("#claims .claim");
    claims.forEach((claim, i) => {
        claim.querySelectorAll("input").forEach(input => {
            input.name = input.name.replace(/\[\d+]/, `[${i}]`);
        });
    });
}