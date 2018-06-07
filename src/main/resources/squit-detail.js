var diff = 'diffPlaceholder';
var diff2html = new Diff2HtmlUI({diff: diff});

$(document).ready(function () {
    var outputToggle = $('#output-toggle');

    outputToggle.click(function () {
        if (outputToggle.text().indexOf("side by side") !== -1) {
            drawDiff('side-by-side');

            outputToggle.text("Show inline");
        } else {
            drawDiff('line-by-line');

            outputToggle.text("Show side by side");
        }
    });

    drawDiff('line-by-line');
});

function drawDiff(outputFormat) {
    diff2html.draw('#diff-view', {
        inputFormat: 'diff',
        outputFormat: outputFormat,
        showFiles: false,
        matching: 'lines'
    });

    $('.d2h-tag').remove();
}
