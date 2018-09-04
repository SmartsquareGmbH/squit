$(document).ready(function () {
    var failedOnlyCheckbox = $("#failed-only");
    var collapseAllButton = $("#collapse-all");
    var expandAllButton = $("#expand-all");

    var collapsibleItems = $(".collapse");
    var containerItems = $(".list-group-item");

    failedOnlyCheckbox.change(function () {
        if (this.checked) {
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
                $(this).prev(".list-group-item").find("svg:first")
                    .removeClass("fa-chevron-right")
                    .addClass("fa-chevron-down");
            }
        })
        .on("hide.bs.collapse hidden.bs.collapse", function (e) {
            if (this === e.target) {
                $(this).prev(".list-group-item").find("svg:first")
                    .removeClass("fa-chevron-down")
                    .addClass("fa-chevron-right");
            }
        });

    containerItems.on("click", function () {
        $(this).next(".list-group").find(".list-group").each(function () {
            $(this).collapse("hide");
        })
    });
});
