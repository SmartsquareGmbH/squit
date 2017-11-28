$(document).ready(function () {
    var diff = 'diffPlaceholder';
    var diff2html = new Diff2HtmlUI({diff: diff});

    diff2html.draw('#diffview', {
        inputFormat: 'diff',
        showFiles: false,
        matching: 'lines'
    });

    $('.d2h-tag').remove();
});
