function getState() {
    try {
        var state = localStorage.getItem("state");

        if (state) {
            return JSON.parse(state);
        } else {
            return {};
        }
    } catch (error) {
        // This browser does not support localStorage.
        return {};
    }
}

function setState(state) {
    try {
        localStorage.setItem("state", JSON.stringify(state));
    } catch (error) {
        // This browser does not support localStorage.
    }
}

$(document).ready(function () {
    var failedOnlyCheckbox = $("#failed-only");
    var collapseAllButton = $("#collapse-all");
    var expandAllButton = $("#expand-all");

    var collapsibleItems = $(".collapse");
    var containerItems = $(".list-group-item");

    function toggleFailedOnly(enable) {
        if (enable) {
            containerItems.each(function () {
                if ($(this).attr("data-success") !== "true") {
                    $(this).css("display", "block");
                } else {
                    $(this).css("display", "none");
                }
            });
        } else {
            containerItems.each(function () {
                $(this).css("display", "block");
            });
        }
    }

    function toggleChevron(element, enable) {
        if (enable) {
            $(element).prev(".list-group-item").find("svg:first")
                .removeClass("fa-chevron-right")
                .addClass("fa-chevron-down");
        } else {
            $(element).prev(".list-group-item").find("svg:first")
                .removeClass("fa-chevron-down")
                .addClass("fa-chevron-right");
        }
    }

    function restoreState() {
        var state = getState();

        toggleFailedOnly(state.failedOnly);
        failedOnlyCheckbox.prop("checked", state.failedOnly);

        for (var stateKey in state) {
            var element = $('#' + stateKey);

            element.addClass("show");

            toggleChevron(element, true);
        }

        $(window).scrollTop(state.position)
    }

    failedOnlyCheckbox.change(function () {
        var state = getState();
        state.failedOnly = this.checked;
        setState(state);

        toggleFailedOnly(this.checked);
    });

    collapseAllButton.click(function () {
        collapsibleItems.collapse("hide");
    });

    expandAllButton.click(function () {
        collapsibleItems.collapse("show");
    });

    collapsibleItems
        .on("show.bs.collapse shown.bs.collapse", function (e) {
            if (this === e.target) {
                var state = getState();
                state[e.target.id] = true;
                setState(state);

                toggleChevron(this, true);
            }
        })
        .on("hide.bs.collapse hidden.bs.collapse", function (e) {
            if (this === e.target) {
                var state = getState();
                delete state[e.target.id];
                setState(state);

                toggleChevron(this, false);
            }
        });

    containerItems.on("click", function () {
        $(this).next(".list-group").find(".list-group").each(function () {
            $(this).collapse("hide");
        })
    });

    restoreState();
});

$(window).on("beforeunload", function () {
    var state = getState();
    state.position = $(window).scrollTop();
    setState(state);
});
