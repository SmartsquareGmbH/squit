var diff = "diffPlaceholder";

var name = "namePlaceholder";
var alternativeName = "alternativeNamePlaceholder";
var description = "descriptionPlaceholder";

var titleElement = $("#title");
var subtitleElement = $("#subtitle");
var descriptionElement = $("#description");
var descriptionContainerElement = $("#description-container");
var outputToggleElement = $("#output-toggle");
var descriptionToggleElement = $("#description-toggle");

var diff2html = new Diff2HtmlUI({diff: diff});

$(document).ready(function () {
    outputToggleElement.click(function () {
        if (outputToggleElement.text().indexOf("side by side") !== -1) {
            drawDiff("side-by-side");

            outputToggleElement.text("Show inline");
        } else {
            drawDiff("line-by-line");

            outputToggleElement.text("Show side by side");
        }
    });

    descriptionContainerElement
        .on("show.bs.collapse shown.bs.collapse", function () {
            descriptionToggleElement.find("svg:first")
                .removeClass("fa-chevron-right")
                .addClass("fa-chevron-down");
        })
        .on("hide.bs.collapse hidden.bs.collapse", function () {
            descriptionToggleElement.find("svg:first")
                .removeClass("fa-chevron-down")
                .addClass("fa-chevron-right");
        });

    drawHeader();
    drawDiff("line-by-line");
});

function drawHeader() {
    if (alternativeName.length > 0) {
        titleElement.text(alternativeName);
        subtitleElement.text(name);
    } else {
        titleElement.text(name);
    }

    if (description) {
        descriptionElement.html(marked(description));
    } else {
        descriptionToggleElement.hide();
    }
}

function drawDiff(outputFormat) {
    diff2html.draw("#diff-view", {
        inputFormat: "diff",
        outputFormat: outputFormat,
        showFiles: false,
        matching: "lines"
    });

    $(".d2h-file-header").remove();
}
