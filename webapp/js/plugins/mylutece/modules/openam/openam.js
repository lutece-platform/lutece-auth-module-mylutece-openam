// displayLoginPagePopup
function displayLoginPagePopup() {
    var currentUrl = window.location.href;
    var returnUrl = currentUrl ? currentUrl : openamBaseUrl();
    returnUrl = addParameter(returnUrl,'checkUserInformations','check');
    return displayLoginPagePopupWithBackUrl(returnUrl);
}

//displayLoginPagePopup
function displayLoginPagePopupWithBackUrl(returnUrl) {
	window.open( popupConnectLoginPageUrl + returnUrl, '_blank', 'width=720,height=812,scrollbars=yes,status=yes,resizable=yes,screenx=0,screeny=0');
    return false;
}

//displayLostPasswordPagePopup
function displayLostPasswordPagePopup() {
	var urlPrefix = "https://accounts.paris.fr/account/send_password?pop=1&returnUrl=";
	var currentUrl = window.location.href;
	var returnUrl = currentUrl ? currentUrl : openamBaseUrl();
	window.open( urlPrefix + returnUrl , '_blank', 'width=720,height=812,scrollbars=yes,status=yes,resizable=yes,screenx=0,screeny=0');
    return false;
}

//base url of the site: the global baseUrl when the theme defines it, else the document base
function openamBaseUrl() {
    return typeof baseUrl !== 'undefined' ? baseUrl : document.baseURI;
}

//value of a field, undefined when the field is absent
function openamFieldValue(id) {
    var field = document.getElementById(id);
    return field ? field.value : undefined;
}

//hides an element when it exists
function openamHide(id) {
    var element = document.getElementById(id);
    if (element) {
        element.style.display = 'none';
    }
}

//shows an element when it exists
function openamShow(id) {
    var element = document.getElementById(id);
    if (element) {
        element.style.display = '';
        if (window.getComputedStyle(element).display === 'none') {
            element.style.display = 'block';
        }
    }
}

document.addEventListener('DOMContentLoaded', function () {

    //Ajax Login authentication
    document.querySelectorAll('.btn-connexion').forEach(function (button) {
        button.addEventListener('click', function (event) {
            event.preventDefault();

            var userInfos = new URLSearchParams();
            ['username', 'password', 'token'].forEach(function (name) {
                var value = openamFieldValue(name);
                if (value !== undefined) {
                    userInfos.append(name, value);
                }
            });

            var urlDoLogin = openamBaseUrl() + "jsp/site/plugins/mylutece/modules/openam/DoLoginJson.jsp";
            fetch(urlDoLogin, {
                method: 'POST',
                body: userInfos,
                cache: 'no-store'
            })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                if (data.status == 'OK') {
                    if (data.result) {
                        openamHide('login_error');
                        location.reload();
                    } else {
                        openamShow('login_error');
                    }
                }
                else if ( data.status == 'ERROR' && ( data.errorCode == 'TOKEN_NOT_VALIDATED'||data.errorCode == 'LOGIN_ERROR') )
                {
                    var token = document.getElementById('token');
                    if (token) {
                        token.value = data.token;
                    }
                    openamShow('login_error');
                }
            })
            .catch(function () {
            });
        });
    });

    openamHide('lost_password');

    var username = document.getElementById('username');
    if (username) {
        username.addEventListener('click', function () {
            openamHide('login_error');
        });
    }

    var password = document.getElementById('password');
    if (password) {
        password.addEventListener('click', function () {
            openamHide('login_error');
        });
        password.addEventListener('focus', function () {
            openamShow('lost_password');
        });
    }

    var lostPassword = document.getElementById('lost_password');
    if (lostPassword) {
        lostPassword.addEventListener('click', function (event) {
            event.preventDefault();
            displayLostPasswordPagePopup();
            openamHide('lost_password');
        });
    }
});

//checked if the user is authenticated
function checkIfUserIsAuthenticated() {
    var result = false;
    var request = new XMLHttpRequest();
    request.open('GET', openamBaseUrl() + 'jsp/site/plugins/mylutece/modules/openam/IsUserAuthenticatedJson.jsp?_=' + Date.now(), false);
    try {
        request.send();
        if (request.status >= 200 && request.status < 300) {
            var data = JSON.parse(request.responseText);
            if (data.status == 'OK') {
                result = data.result;
            }
        }
    } catch (e) {
    }
    return result;
}

function addParameter(uri, key, value) {
	  var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
	  var separator = uri.indexOf('?') !== -1 ? "&" : "?";
	  if (uri.match(re)) {
	    return uri.replace(re, '$1' + key + "=" + value + '$2');
	  }
	  else {
	    return uri + separator + key + "=" + value;
	  }
	}
