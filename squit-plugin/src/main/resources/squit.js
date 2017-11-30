$(document).ready(function () {
    var failedOnlyCheckbox = $('#failed-only');
    var items = $('.list-group-item');

    items.on('click', function () {
        $('.fa', this)
            .toggleClass('fa-chevron-right')
            .toggleClass('fa-chevron-down');
    });

    failedOnlyCheckbox.change(function () {
        if (this.checked) {
            items.each(function () {
                if ($(this).attr('data-success') !== 'true') {
                    $(this).css("display", "block");
                } else {
                    $(this).css("display", "none");
                }
            });
        } else {
            items.each(function () {
                $(this).css("display", "block");
            });
        }
    });
});
