(() => {
    const selector = "vaadin-connection-indicator";

    function configureIndicator(element) {
        element.setAttribute("role", "region");
        element.setAttribute("aria-label", "Status koneksi aplikasi");
    }

    function configureAll() {
        document.querySelectorAll(selector)
            .forEach(configureIndicator);
    }

    function initialize() {
        configureAll();

        const observer = new MutationObserver(records => {
            for (const record of records) {
                for (const node of record.addedNodes) {
                    if (node.nodeType !== 1) {
                        continue;
                    }

                    if (node.matches(selector)) {
                        configureIndicator(node);
                    }

                    node.querySelectorAll(selector)
                        .forEach(configureIndicator);
                }
            }
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded", initialize, { once: true }
        );
    } else {
        initialize();
    }
})();