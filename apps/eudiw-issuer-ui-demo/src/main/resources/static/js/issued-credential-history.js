(() => {
    "use strict";

    const STORAGE_KEY = "bevisgenerator.issued-credentials.v1";
    const STORAGE_VERSION = 1;
    const MAX_CREDENTIALS = 20;
    const DATE_TIME_FORMATTER = new Intl.DateTimeFormat("nn-NO", {
        dateStyle: "medium",
        timeStyle: "short"
    });

    const isObject = value => typeof value === "object" && value !== null && !Array.isArray(value);
    const isNonBlankString = value => typeof value === "string" && value.trim().length > 0;
    const isValidDate = value => typeof value === "string" && Number.isFinite(Date.parse(value));

    const storageErrorMessage = action =>
        `Klarte ikkje ${action} den lokale bevishistorikken. Du kan framleis bruke skjemaet manuelt.`;

    const validateCredential = credential => {
        if (!isObject(credential)
            || !isNonBlankString(credential.credentialConfigurationId)
            || !isNonBlankString(credential.credentialDescription)
            || !isNonBlankString(credential.issuanceTransactionId)
            || !isValidDate(credential.issuedAt)
            || !(credential.subjectIdentifier === null || isNonBlankString(credential.subjectIdentifier))) {
            return null;
        }

        return {
            credentialConfigurationId: credential.credentialConfigurationId,
            credentialDescription: credential.credentialDescription,
            issuanceTransactionId: credential.issuanceTransactionId,
            subjectIdentifier: credential.subjectIdentifier,
            issuedAt: credential.issuedAt
        };
    };

    const readCredentials = () => {
        const serialized = window.localStorage.getItem(STORAGE_KEY);
        if (serialized === null) {
            return [];
        }

        const history = JSON.parse(serialized);
        if (!isObject(history)
            || history.version !== STORAGE_VERSION
            || !Array.isArray(history.credentials)) {
            throw new Error("Invalid credential history structure");
        }

        return history.credentials.map(validateCredential).filter(credential => credential !== null);
    };

    const writeCredentials = credentials =>
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
            version: STORAGE_VERSION,
            credentials
        }));

    const saveIssuedCredential = credential => {
        const validCredential = validateCredential(credential);
        if (validCredential === null) {
            throw new Error("Invalid credential history entry");
        }

        const credentials = readCredentials();
        if (credentials.some(existing =>
            existing.issuanceTransactionId === validCredential.issuanceTransactionId)) {
            return;
        }

        writeCredentials([validCredential, ...credentials]
            .sort((left, right) => Date.parse(right.issuedAt) - Date.parse(left.issuedAt))
            .slice(0, MAX_CREDENTIALS));
    };

    const removeRevokedCredentials = ({
        credentialConfigurationId,
        issuanceTransactionId,
        subjectIdentifier
    }) => {
        const remaining = readCredentials().filter(credential => {
            if (isNonBlankString(issuanceTransactionId)) {
                return credential.issuanceTransactionId !== issuanceTransactionId;
            }
            return !(credential.credentialConfigurationId === credentialConfigurationId
                && credential.subjectIdentifier === subjectIdentifier);
        });

        if (remaining.length > 0) {
            writeCredentials(remaining);
        } else {
            clearCredentials();
        }
    };

    const clearCredentials = () => window.localStorage.removeItem(STORAGE_KEY);

    const formatDateTime = value => DATE_TIME_FORMATTER.format(new Date(value));

    const setFormValue = (element, value, eventName) => {
        if (element instanceof HTMLSelectElement
            && !Array.from(element.options).some(option => option.value === value)) {
            return false;
        }

        element.value = value;
        element.dispatchEvent(new Event(eventName, {bubbles: true}));
        return true;
    };

    document.addEventListener("alpine:init", () => {
        Alpine.data("issuanceCompletionHistory", () => ({
            storageError: "",

            init() {
                const subjectIdentifier = this.$root.dataset.subjectIdentifier;
                const credential = {
                    credentialConfigurationId: this.$root.dataset.credentialConfigurationId,
                    credentialDescription: this.$root.dataset.credentialDescription,
                    issuanceTransactionId: this.$root.dataset.issuanceTransactionId,
                    subjectIdentifier: isNonBlankString(subjectIdentifier) ? subjectIdentifier : null,
                    issuedAt: new Date().toISOString()
                };

                try {
                    saveIssuedCredential(credential);
                } catch {
                    this.storageError = storageErrorMessage("lagre");
                }
            }
        }));

        Alpine.data("revocationResultHistory", () => ({
            storageError: "",

            init() {
                const {credentialConfigurationId, issuanceTransactionId, subjectIdentifier} = this.$root.dataset;
                if (!isNonBlankString(issuanceTransactionId)
                    && !(isNonBlankString(credentialConfigurationId) && isNonBlankString(subjectIdentifier))) {
                    return;
                }

                try {
                    removeRevokedCredentials({credentialConfigurationId, issuanceTransactionId, subjectIdentifier});
                } catch {
                    this.storageError = storageErrorMessage("oppdatere");
                }
            }
        }));

        Alpine.data("revocationPage", () => ({
            selectedForm: "",
            credentials: [],
            selectedCredentialTransactionId: "",
            prefilledCredential: null,
            prefilledForm: "",
            historyError: "",
            pickerTrigger: null,

            get historySummary() {
                const count = this.credentials.length;
                return count === 1
                    ? "Du har oppretta 1 bevis"
                    : `Du har oppretta ${count} bevis`;
            },

            get prefillMessage() {
                if (this.prefilledCredential === null) {
                    return "";
                }
                const suffix = this.prefilledForm === "revoke-by-subject-form"
                    ? " Tilbakekallinga kan gjelde fleire bevis."
                    : "";
                return `Skjemaet er fylt ut frå ${this.prefilledCredential.credentialDescription}, utferda ${formatDateTime(this.prefilledCredential.issuedAt)}.${suffix}`;
            },

            init() {
                const initialMethod = this.$root.dataset.initialRevocationMethod;
                this.selectedForm = initialMethod === "transaction-id"
                    ? "revoke-form"
                    : initialMethod === "person-identifier"
                        ? "revoke-by-subject-form"
                        : "";
                this.loadCredentials();
            },

            loadCredentials() {
                try {
                    this.credentials = readCredentials();
                    this.historyError = "";
                } catch {
                    this.credentials = [];
                    this.historyError = storageErrorMessage("lese");
                }
            },

            openCredentialPicker(event) {
                this.loadCredentials();
                if (this.credentials.length === 0) {
                    return;
                }

                this.pickerTrigger = event.currentTarget;
                this.selectedCredentialTransactionId = this.credentials[0].issuanceTransactionId;
                this.$nextTick(() => {
                    if (!this.$refs.credentialPicker.open) {
                        this.$refs.credentialPicker.showModal();
                    }
                });
            },

            closeCredentialPicker() {
                this.$refs.credentialPicker.close();
            },

            restorePickerFocus() {
                const trigger = this.pickerTrigger;
                this.pickerTrigger = null;
                this.$nextTick(() => trigger?.focus());
            },

            useSelectedCredential() {
                const credential = this.credentials.find(candidate =>
                    candidate.issuanceTransactionId === this.selectedCredentialTransactionId);
                if (credential === undefined) {
                    return;
                }

                const targetForm = this.selectedForm === "revoke-by-subject-form"
                    && isNonBlankString(credential.subjectIdentifier)
                    ? "revoke-by-subject-form"
                    : "revoke-form";
                this.selectedForm = targetForm;

                const selectedConfiguration = targetForm === "revoke-by-subject-form"
                    ? this.$refs.subjectCredentialConfigurationId
                    : this.$refs.transactionCredentialConfigurationId;
                if (!setFormValue(
                    selectedConfiguration,
                    credential.credentialConfigurationId,
                    "change"
                )) {
                    this.historyError = "Bevistypen finst ikkje lenger. Vel ein annan bevistype eller slett den lokale historikken.";
                    this.closeCredentialPicker();
                    return;
                }

                if (targetForm === "revoke-by-subject-form") {
                    setFormValue(this.$refs.subjectIdentifier, credential.subjectIdentifier, "input");
                } else {
                    setFormValue(this.$refs.issuanceTransactionId, credential.issuanceTransactionId, "input");
                }

                this.prefilledCredential = credential;
                this.prefilledForm = targetForm;
                this.historyError = "";
                this.pickerTrigger = targetForm === "revoke-by-subject-form"
                    ? this.$refs.subjectIdentifier
                    : this.$refs.issuanceTransactionId;
                this.closeCredentialPicker();
            },

            clearLocalHistory() {
                try {
                    clearCredentials();
                    const focusTarget = this.selectedForm === "revoke-by-subject-form"
                        ? this.$refs.subjectMethod
                        : this.$refs.transactionMethod;
                    const pickerWasOpen = this.$refs.credentialPicker.open;
                    this.credentials = [];
                    this.prefilledCredential = null;
                    this.prefilledForm = "";
                    this.historyError = "";
                    if (pickerWasOpen) {
                        this.pickerTrigger = focusTarget;
                        this.closeCredentialPicker();
                    } else {
                        this.$nextTick(() => focusTarget.focus());
                    }
                } catch {
                    this.historyError = storageErrorMessage("slette");
                    if (this.$refs.credentialPicker.open) {
                        this.closeCredentialPicker();
                    }
                }
            },

            formatDateTime
        }));
    });
})();
