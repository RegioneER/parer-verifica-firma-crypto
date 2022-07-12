function scrollFunction() {
	if (document.body.scrollTop > 60 || document.documentElement.scrollTop > 60) {
		$("#btnup").show();
	} else {
		$("#btnup").hide();
	}
}

// When the user clicks on the button, scroll to the top of the document
function topFunction() {
	$("html, body").animate({
		scrollTop : 0
	}, 800);
}


function filter (element,what) {
    var value = $(element).val().toLowerCase();
    
    if (value == '') {
        $('#'+what+' > li').show();
    }
    else {
    	 $('#'+what+' > li').filter(function() {
    	        let item = $(this).text().toLowerCase().indexOf(value) > -1;
    	        $(this).toggle(item);
    	    });
    }
}